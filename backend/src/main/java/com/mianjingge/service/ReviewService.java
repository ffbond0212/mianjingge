package com.mianjingge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 复习计划：遗忘曲线排期。
 * 入队（手动/面试低分自动）→ 按间隔排 next_review_date → 今日到期打卡 → 答上进档×1.5 / 答差降档 → 五档走完归档。
 * Redis ZSet 按下次复习时间排序（member=planId，score=epochDay），缺失时从库重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    public static final String ZSET_KEY = "mjg:review:queue";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final List<Integer> DEFAULT_INTERVALS = List.of(1, 3, 7, 15, 30);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ReviewPlanMapper reviewPlanMapper;
    private final ReviewConfigMapper reviewConfigMapper;
    private final QuestionMapper questionMapper;
    private final StringRedisTemplate redisTemplate;

    // ---------- 全局间隔配置 ----------

    /** 读取间隔配置；缺失/损坏时回落默认 1/3/7/15/30 */
    public List<Integer> getIntervals() {
        ReviewConfig config = reviewConfigMapper.selectById(1);
        if (config == null) {
            return DEFAULT_INTERVALS;
        }
        try {
            List<Integer> intervals = JSON.readValue(config.getIntervalsJson(), new TypeReference<List<Integer>>() {});
            if (intervals != null && intervals.size() >= 2) {
                return intervals;
            }
        } catch (Exception e) {
            log.warn("复习间隔配置解析失败，回落默认值: {}", e.getMessage());
        }
        return DEFAULT_INTERVALS;
    }

    public void updateIntervals(List<Integer> intervals) {
        validateIntervals(intervals);
        ReviewConfig config = reviewConfigMapper.selectById(1);
        if (config == null) {
            config = new ReviewConfig();
            config.setId(1);
            config.setIntervalsJson(writeJson(intervals));
            reviewConfigMapper.insert(config);
        } else {
            config.setIntervalsJson(writeJson(intervals));
            reviewConfigMapper.updateById(config);
        }
    }

    private void validateIntervals(List<Integer> intervals) {
        if (intervals == null || intervals.size() < 2 || intervals.size() > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "间隔需 2-10 档");
        }
        for (int i = 0; i < intervals.size(); i++) {
            int v = intervals.get(i);
            if (v < 1 || v > 365) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "每档间隔需在 1-365 天");
            }
            if (i > 0 && v <= intervals.get(i - 1)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "间隔必须逐档递增");
            }
        }
    }

    private String writeJson(List<Integer> intervals) {
        try {
            return JSON.writeValueAsString(intervals);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "间隔配置序列化失败");
        }
    }

    // ---------- 查询 ----------

    /** 今日到期任务：全部到期不设上限，按下次复习时间排序 */
    public List<ReviewPlanVO> today() {
        ensureQueueBuilt();
        Set<String> ids = null;
        try {
            ids = redisTemplate.opsForZSet().rangeByScore(ZSET_KEY, 0, (double) LocalDate.now().toEpochDay());
        } catch (Exception e) {
            log.warn("Redis 不可用，回退数据库查询今日任务: {}", e.getMessage());
        }
        List<ReviewPlan> plans;
        if (ids == null) {
            plans = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                    .eq(ReviewPlan::getStatus, STATUS_ACTIVE)
                    .le(ReviewPlan::getNextReviewDate, LocalDate.now())
                    .orderByAsc(ReviewPlan::getNextReviewDate)
                    .orderByAsc(ReviewPlan::getId));
        } else if (ids.isEmpty()) {
            return List.of();
        } else {
            Map<Long, ReviewPlan> byId = reviewPlanMapper.selectBatchIds(
                            ids.stream().map(Long::valueOf).toList())
                    .stream()
                    .collect(Collectors.toMap(ReviewPlan::getId, p -> p));
            plans = ids.stream().map(Long::valueOf).map(byId::get).filter(Objects::nonNull).toList();
        }
        return toVOs(plans);
    }

    /** 全部进行中的计划，按下次复习时间升序 */
    public List<ReviewPlanVO> listPlans() {
        List<ReviewPlan> plans = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getStatus, STATUS_ACTIVE)
                .orderByAsc(ReviewPlan::getNextReviewDate)
                .orderByAsc(ReviewPlan::getId));
        return toVOs(plans);
    }

    // ---------- 入队 ----------

    /** 手动入队：已在队列中报错；毕业归档过的题重置回第一档 */
    public ReviewPlanVO enqueue(Long questionId) {
        Question question = requireQuestion(questionId);
        ReviewPlan active = activePlanOf(questionId);
        if (active != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该题已在复习队列中");
        }
        ReviewPlan completed = completedPlanOf(questionId);
        ReviewPlan plan;
        if (completed != null) {
            resetPlan(completed, question.getTitle());
            reviewPlanMapper.updateById(completed);
            plan = completed;
        } else {
            plan = buildPlan(questionId, question.getTitle());
            reviewPlanMapper.insert(plan);
        }
        queueAdd(plan);
        return toVOs(List.of(plan)).get(0);
    }

    /** 自动入队（面试记录写入触发）：低分/未答上才入队，已有计划不重复 */
    public void autoEnqueue(Long questionId, String title, Integer score, Integer answered) {
        if (questionId == null) {
            return;
        }
        boolean bad = (score != null && score < 6) || (answered != null && answered == 3);
        if (!bad || activePlanOf(questionId) != null) {
            return;
        }
        if (questionMapper.selectById(questionId) == null) {
            return; // 题目已删除，不入队
        }
        ReviewPlan completed = completedPlanOf(questionId);
        ReviewPlan plan;
        if (completed != null) {
            resetPlan(completed, title);
            reviewPlanMapper.updateById(completed);
            plan = completed;
        } else {
            plan = buildPlan(questionId, title);
            reviewPlanMapper.insert(plan);
        }
        queueAdd(plan);
    }

    private ReviewPlan buildPlan(Long questionId, String title) {
        ReviewPlan plan = new ReviewPlan();
        plan.setQuestionId(questionId);
        plan.setQuestionSnapshot(title);
        resetPlan(plan, title);
        return plan;
    }

    /** 重置到第一档：间隔取配置第 0 档，下次复习 = 今天 + 间隔 */
    private void resetPlan(ReviewPlan plan, String title) {
        int interval = getIntervals().get(0);
        plan.setQuestionSnapshot(title);
        plan.setStage(0);
        plan.setIntervalDays(interval);
        plan.setNextReviewDate(LocalDate.now().plusDays(interval));
        plan.setStatus(STATUS_ACTIVE);
        plan.setReviewCount(0);
        plan.setLastScore(null);
    }

    // ---------- 打卡 ----------

    /** 打卡：答上→下一档间隔×1.5 延后；答差→降回上一档；五档走完归档毕业 */
    public ReviewPlanVO checkin(Long id, ReviewCheckinRequest request) {
        ReviewPlan plan = requirePlan(id);
        if (!STATUS_ACTIVE.equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该计划已毕业归档");
        }
        int answered = request.getAnswered();
        List<Integer> intervals = getIntervals();
        LocalDate today = LocalDate.now();

        if (answered == 1) {
            int nextStage = plan.getStage() + 1;
            if (nextStage >= intervals.size()) {
                plan.setStatus(STATUS_COMPLETED);
                plan.setStage(nextStage);
                plan.setIntervalDays(0);
                plan.setNextReviewDate(today);
            } else {
                int interval = Math.max(1, (int) Math.round(intervals.get(nextStage) * 1.5));
                plan.setStage(nextStage);
                plan.setIntervalDays(interval);
                plan.setNextReviewDate(today.plusDays(interval));
            }
        } else {
            int prevStage = Math.max(0, plan.getStage() - 1);
            plan.setStage(prevStage);
            plan.setIntervalDays(intervals.get(prevStage));
            plan.setNextReviewDate(today.plusDays(intervals.get(prevStage)));
        }
        plan.setReviewCount(plan.getReviewCount() + 1);
        plan.setLastScore(request.getScore());
        reviewPlanMapper.updateById(plan);

        if (STATUS_COMPLETED.equals(plan.getStatus())) {
            queueRemove(plan.getId());
        } else {
            queueAdd(plan);
        }
        return toVOs(List.of(plan)).get(0);
    }

    // ---------- 移出 ----------

    public void removePlan(Long id) {
        requirePlan(id);
        reviewPlanMapper.deleteById(id);
        queueRemove(id);
    }

    // ---------- Redis ZSet ----------

    /** 缓存丢失时从库重建（ZSet 为空时 Redis 会删除 key，属正常） */
    private void ensureQueueBuilt() {
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(ZSET_KEY))) {
                return;
            }
            List<ReviewPlan> actives = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                    .eq(ReviewPlan::getStatus, STATUS_ACTIVE));
            for (ReviewPlan p : actives) {
                redisTemplate.opsForZSet().add(ZSET_KEY, String.valueOf(p.getId()), p.getNextReviewDate().toEpochDay());
            }
        } catch (Exception e) {
            log.warn("复习队列缓存重建失败（Redis 不可用）: {}", e.getMessage());
        }
    }

    private void queueAdd(ReviewPlan plan) {
        try {
            redisTemplate.opsForZSet().add(ZSET_KEY, String.valueOf(plan.getId()), plan.getNextReviewDate().toEpochDay());
        } catch (Exception e) {
            log.warn("复习队列缓存写入失败（Redis 不可用）: {}", e.getMessage());
        }
    }

    private void queueRemove(Long planId) {
        try {
            redisTemplate.opsForZSet().remove(ZSET_KEY, String.valueOf(planId));
        } catch (Exception e) {
            log.warn("复习队列缓存删除失败（Redis 不可用）: {}", e.getMessage());
        }
    }

    // ---------- 私有工具 ----------

    private ReviewPlan activePlanOf(Long questionId) {
        return firstOf(questionId, STATUS_ACTIVE);
    }

    private ReviewPlan completedPlanOf(Long questionId) {
        return firstOf(questionId, STATUS_COMPLETED);
    }

    private ReviewPlan firstOf(Long questionId, String status) {
        List<ReviewPlan> plans = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getQuestionId, questionId)
                .eq(ReviewPlan::getStatus, status)
                .orderByDesc(ReviewPlan::getId)
                .last("LIMIT 1"));
        return plans.isEmpty() ? null : plans.get(0);
    }

    private Question requireQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private ReviewPlan requirePlan(Long id) {
        ReviewPlan plan = reviewPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "复习计划不存在");
        }
        return plan;
    }

    /** 批量换算 VO：一次查询补齐题目标题与分类 */
    private List<ReviewPlanVO> toVOs(List<ReviewPlan> plans) {
        if (plans.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = plans.stream().map(ReviewPlan::getQuestionId).distinct().toList();
        Map<Long, Question> questions = questionIds.isEmpty()
                ? Map.of()
                : questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));
        return plans.stream().map(p -> toVO(p, questions.get(p.getQuestionId()))).toList();
    }

    private ReviewPlanVO toVO(ReviewPlan p, Question q) {
        ReviewPlanVO vo = new ReviewPlanVO();
        vo.setId(p.getId());
        vo.setQuestionId(p.getQuestionId());
        vo.setQuestionTitle(q != null ? q.getTitle() : p.getQuestionSnapshot());
        vo.setCategory(q != null ? q.getCategory() : null);
        vo.setCategoryLabel(q != null && q.getCategory() != null ? q.getCategory().getLabel() : null);
        vo.setStage(p.getStage());
        vo.setStageLabel(STATUS_COMPLETED.equals(p.getStatus()) ? "已毕业" : "第" + (p.getStage() + 1) + "轮");
        vo.setIntervalDays(p.getIntervalDays());
        vo.setNextReviewDate(p.getNextReviewDate());
        vo.setReviewCount(p.getReviewCount());
        vo.setLastScore(p.getLastScore());
        vo.setStatus(p.getStatus());
        return vo;
    }
}
