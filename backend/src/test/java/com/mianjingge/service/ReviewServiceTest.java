package com.mianjingge.service;

import com.mianjingge.common.BusinessException;
import com.mianjingge.common.ErrorCode;
import com.mianjingge.dto.ReviewCheckinRequest;
import com.mianjingge.entity.Question;
import com.mianjingge.entity.ReviewConfig;
import com.mianjingge.entity.ReviewPlan;
import com.mianjingge.mapper.QuestionMapper;
import com.mianjingge.mapper.ReviewConfigMapper;
import com.mianjingge.mapper.ReviewPlanMapper;
import com.mianjingge.vo.ReviewPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 遗忘曲线排期规则与入队逻辑的单元测试 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewPlanMapper reviewPlanMapper;
    @Mock
    private ReviewConfigMapper reviewConfigMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zsetOperations;

    @InjectMocks
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        // 默认五档间隔
        ReviewConfig config = new ReviewConfig();
        config.setId(1);
        config.setIntervalsJson("[1,3,7,15,30]");
        when(reviewConfigMapper.selectById(1)).thenReturn(config);
        // toVOs 批量补题目信息（无题目时回落快照）
        when(questionMapper.selectBatchIds(anyList())).thenReturn(List.of());
        when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    }

    private ReviewPlan plan(int stage) {
        ReviewPlan p = new ReviewPlan();
        p.setId(1L);
        p.setQuestionId(10L);
        p.setQuestionSnapshot("快照题");
        p.setStage(stage);
        p.setIntervalDays(1);
        p.setNextReviewDate(LocalDate.now());
        p.setStatus(ReviewService.STATUS_ACTIVE);
        p.setReviewCount(0);
        return p;
    }

    private ReviewCheckinRequest checkin(int answered, Integer score) {
        ReviewCheckinRequest request = new ReviewCheckinRequest();
        request.setAnswered(answered);
        request.setScore(score);
        return request;
    }

    // ---------- 打卡排期 ----------

    @Test
    void checkin_answeredWell_advancesStageWithOnePointFiveMultiplier() {
        when(reviewPlanMapper.selectById(1L)).thenReturn(plan(0));

        ReviewPlanVO vo = reviewService.checkin(1L, checkin(1, 8));

        assertThat(vo.getStage()).isEqualTo(1);
        assertThat(vo.getIntervalDays()).isEqualTo(5); // round(3 × 1.5)
        assertThat(vo.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(vo.getStatus()).isEqualTo(ReviewService.STATUS_ACTIVE);
    }

    @Test
    void checkin_answeredWell_secondStageIntervalIs11() {
        when(reviewPlanMapper.selectById(1L)).thenReturn(plan(1));

        ReviewPlanVO vo = reviewService.checkin(1L, checkin(1, null));

        assertThat(vo.getStage()).isEqualTo(2);
        assertThat(vo.getIntervalDays()).isEqualTo(11); // round(7 × 1.5)
    }

    @Test
    void checkin_answeredPoor_demotesOneStage() {
        when(reviewPlanMapper.selectById(1L)).thenReturn(plan(2));

        ReviewPlanVO vo = reviewService.checkin(1L, checkin(2, 4));

        assertThat(vo.getStage()).isEqualTo(1);
        assertThat(vo.getIntervalDays()).isEqualTo(3); // 降档回原间隔，不乘 1.5
    }

    @Test
    void checkin_answeredPoor_atStage0_staysAtStage0() {
        when(reviewPlanMapper.selectById(1L)).thenReturn(plan(0));

        ReviewPlanVO vo = reviewService.checkin(1L, checkin(3, null));

        assertThat(vo.getStage()).isEqualTo(0);
        assertThat(vo.getIntervalDays()).isEqualTo(1);
        assertThat(vo.getLastScore()).isNull();
    }

    @Test
    void checkin_finishingFifthStage_completesPlan() {
        when(reviewPlanMapper.selectById(1L)).thenReturn(plan(4));

        ReviewPlanVO vo = reviewService.checkin(1L, checkin(1, 9));

        assertThat(vo.getStatus()).isEqualTo(ReviewService.STATUS_COMPLETED);
        assertThat(vo.getStageLabel()).isEqualTo("已毕业");
        assertThat(vo.getIntervalDays()).isZero();
        verify(zsetOperations).remove(any(), any());
        verify(zsetOperations, never()).add(any(), any(), any(double.class));
    }

    @Test
    void checkin_completedPlan_isRejected() {
        ReviewPlan completed = plan(4);
        completed.setStatus(ReviewService.STATUS_COMPLETED);
        when(reviewPlanMapper.selectById(1L)).thenReturn(completed);

        assertThatThrownBy(() -> reviewService.checkin(1L, checkin(1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已毕业归档");
    }

    @Test
    void checkin_missingPlan_throwsNotFound() {
        when(reviewPlanMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.checkin(1L, checkin(1, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ---------- 间隔配置 ----------

    @Test
    void getIntervals_fallsBackToDefaultWhenConfigMissingOrCorrupt() {
        when(reviewConfigMapper.selectById(1)).thenReturn(null);
        assertThat(reviewService.getIntervals()).containsExactly(1, 3, 7, 15, 30);

        ReviewConfig corrupt = new ReviewConfig();
        corrupt.setId(1);
        corrupt.setIntervalsJson("[1,3,x");
        when(reviewConfigMapper.selectById(1)).thenReturn(corrupt);
        assertThat(reviewService.getIntervals()).containsExactly(1, 3, 7, 15, 30);
    }

    @Test
    void updateIntervals_validatesOrderAndCount() {
        assertThatThrownBy(() -> reviewService.updateIntervals(List.of(3, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("递增");
        assertThatThrownBy(() -> reviewService.updateIntervals(List.of(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2-10 档");
        assertThatThrownBy(() -> reviewService.updateIntervals(List.of(1, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1-365");
    }

    // ---------- 手动入队 ----------

    @Test
    void enqueue_createsNewPlanAtStage0() {
        when(reviewPlanMapper.selectList(any())).thenReturn(List.of());
        Question question = new Question();
        question.setId(10L);
        question.setTitle("真实题");
        when(questionMapper.selectById(10L)).thenReturn(question);
        when(questionMapper.selectBatchIds(anyList())).thenReturn(List.of(question));

        ReviewPlanVO vo = reviewService.enqueue(10L);

        assertThat(vo.getStage()).isEqualTo(0);
        assertThat(vo.getIntervalDays()).isEqualTo(1);
        assertThat(vo.getQuestionTitle()).isEqualTo("真实题");
        assertThat(vo.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
        verify(reviewPlanMapper).insert(any(ReviewPlan.class));
    }

    @Test
    void enqueue_rejectsWhenAlreadyActive() {
        when(reviewPlanMapper.selectList(any())).thenReturn(List.of(plan(0)));
        when(questionMapper.selectById(10L)).thenReturn(new Question());

        assertThatThrownBy(() -> reviewService.enqueue(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已在复习队列中");
        verify(reviewPlanMapper, never()).insert(any(ReviewPlan.class));
    }

    @Test
    void enqueue_resetsCompletedPlanBackToStage0() {
        ReviewPlan completed = plan(5);
        completed.setStatus(ReviewService.STATUS_COMPLETED);
        when(reviewPlanMapper.selectList(any())).thenReturn(List.of(), List.of(completed));
        Question question = new Question();
        question.setId(10L);
        question.setTitle("真实题");
        when(questionMapper.selectById(10L)).thenReturn(question);
        when(questionMapper.selectBatchIds(anyList())).thenReturn(List.of(question));

        ReviewPlanVO vo = reviewService.enqueue(10L);

        assertThat(vo.getStatus()).isEqualTo(ReviewService.STATUS_ACTIVE);
        assertThat(vo.getStage()).isEqualTo(0);
        ArgumentCaptor<ReviewPlan> captor = ArgumentCaptor.forClass(ReviewPlan.class);
        verify(reviewPlanMapper).updateById(captor.capture());
        assertThat(captor.getValue().getReviewCount()).isZero();
    }

    // ---------- 自动入队（面试记录触发） ----------

    @Test
    void autoEnqueue_skipsWhenScoreIsGood() {
        reviewService.autoEnqueue(10L, "题", 8, 1);
        verify(reviewPlanMapper, never()).insert(any(ReviewPlan.class));
    }

    @Test
    void autoEnqueue_createsWhenScoreIsLowOrUnanswered() {
        when(reviewPlanMapper.selectList(any())).thenReturn(List.of());
        when(questionMapper.selectById(10L)).thenReturn(new Question());

        reviewService.autoEnqueue(10L, "题", 5, 1);
        reviewService.autoEnqueue(10L, "题", null, 3);

        verify(reviewPlanMapper, org.mockito.Mockito.times(2)).insert(any(ReviewPlan.class));
    }

    @Test
    void autoEnqueue_skipsWhenPlanAlreadyExists() {
        when(reviewPlanMapper.selectList(any())).thenReturn(List.of(plan(0)));

        reviewService.autoEnqueue(10L, "题", 3, 1);

        verify(reviewPlanMapper, never()).insert(any(ReviewPlan.class));
        verify(questionMapper, never()).selectById(any());
    }

    @Test
    void autoEnqueue_skipsWhenQuestionDeleted() {
        when(reviewPlanMapper.selectList(any())).thenReturn(List.of());
        when(questionMapper.selectById(10L)).thenReturn(null);

        reviewService.autoEnqueue(10L, "题", 2, 1);

        verify(reviewPlanMapper, never()).insert(any(ReviewPlan.class));
    }
}
