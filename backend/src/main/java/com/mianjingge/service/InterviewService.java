package com.mianjingge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mianjingge.common.BusinessException;
import com.mianjingge.common.ErrorCode;
import com.mianjingge.common.PageResult;
import com.mianjingge.dto.InterviewQueryRequest;
import com.mianjingge.dto.InterviewQuestionSaveRequest;
import com.mianjingge.dto.InterviewSaveRequest;
import com.mianjingge.entity.Interview;
import com.mianjingge.entity.InterviewQuestion;
import com.mianjingge.entity.Question;
import com.mianjingge.mapper.InterviewMapper;
import com.mianjingge.mapper.InterviewQuestionMapper;
import com.mianjingge.mapper.QuestionMapper;
import com.mianjingge.vo.InterviewDetailVO;
import com.mianjingge.vo.InterviewQuestionVO;
import com.mianjingge.vo.InterviewVO;
import com.mianjingge.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewService {

    /** 是否答上：1 答上 2 部分答上 3 未答上 */
    public static final int ANSWERED_FULL = 1;
    public static final int ANSWERED_PARTIAL = 2;
    public static final int ANSWERED_NOT = 3;

    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final QuestionMapper questionMapper;
    private final StatsService statsService;
    private final ReviewService reviewService;

    /** 分页查询场次：关键词模糊匹配 公司/岗位/轮次 */
    public PageResult<InterviewVO> page(InterviewQueryRequest request) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        String keyword = request.getKeyword();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Interview::getCompany, keyword)
                    .or().like(Interview::getPosition, keyword)
                    .or().like(Interview::getRound, keyword));
        }
        wrapper.orderByDesc(Interview::getInterviewDate).orderByDesc(Interview::getId);

        Page<Interview> page = interviewMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        return PageResult.of(page.getTotal(), toVOs(page.getRecords()));
    }

    public InterviewDetailVO detail(Long id) {
        Interview interview = requireInterview(id);
        List<InterviewQuestion> records = interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getInterviewId, id)
                        .orderByAsc(InterviewQuestion::getId));
        return new InterviewDetailVO(toVO(interview, records), toQuestionVOs(records));
    }

    public InterviewVO create(InterviewSaveRequest request) {
        Interview interview = new Interview();
        apply(interview, request);
        interviewMapper.insert(interview);
        statsService.invalidate();
        return toVO(interview, List.of());
    }

    public InterviewVO update(Long id, InterviewSaveRequest request) {
        Interview interview = requireInterview(id);
        apply(interview, request);
        interviewMapper.updateById(interview);
        statsService.invalidate();
        return toVO(interview, listRecords(id));
    }

    /** 删除场次：级联删除题目记录，不影响题库题目 */
    public void delete(Long id) {
        requireInterview(id);
        interviewQuestionMapper.delete(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getInterviewId, id));
        interviewMapper.deleteById(id);
        statsService.invalidate();
    }

    /** 添加题目记录：可关联题库已有题（questionId），或现场新题只存快照 */
    public InterviewQuestionVO addQuestion(Long interviewId, InterviewQuestionSaveRequest request) {
        requireInterview(interviewId);
        InterviewQuestion record = new InterviewQuestion();
        record.setInterviewId(interviewId);
        if (request.getQuestionId() != null) {
            Question question = questionMapper.selectById(request.getQuestionId());
            if (question == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "关联题目不存在");
            }
            record.setQuestionId(request.getQuestionId());
        }
        apply(record, request);
        interviewQuestionMapper.insert(record);
        statsService.invalidate();
        reviewService.autoEnqueue(record.getQuestionId(), request.getQuestionSnapshot(), record.getScore(), record.getAnswered());
        return toQuestionVO(record, titleOf(record.getQuestionId()));
    }

    public InterviewQuestionVO updateQuestion(Long interviewId, Long recordId, InterviewQuestionSaveRequest request) {
        InterviewQuestion record = requireRecord(interviewId, recordId);
        // 关联题目创建后不可变更，防止篡改
        apply(record, request);
        interviewQuestionMapper.updateById(record);
        statsService.invalidate();
        reviewService.autoEnqueue(record.getQuestionId(), request.getQuestionSnapshot(), record.getScore(), record.getAnswered());
        return toQuestionVO(record, titleOf(record.getQuestionId()));
    }

    public void deleteQuestion(Long interviewId, Long recordId) {
        requireRecord(interviewId, recordId);
        interviewQuestionMapper.deleteById(recordId);
        statsService.invalidate();
    }

    /** 把现场回答快照保存为对应题库题目的个人话术 */
    public QuestionVO saveAsPhrase(Long interviewId, Long recordId) {
        InterviewQuestion record = requireRecord(interviewId, recordId);
        if (record.getQuestionId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该记录未关联题库题目，无法保存话术");
        }
        if (!StringUtils.hasText(record.getMyAnswerSnapshot())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "现场回答为空，无可保存的话术");
        }
        Question question = questionMapper.selectById(record.getQuestionId());
        question.setMyAnswer(record.getMyAnswerSnapshot());
        questionMapper.updateById(question);
        return QuestionVO.from(question);
    }

    // ---------- 私有工具 ----------

    private void apply(Interview interview, InterviewSaveRequest request) {
        interview.setCompany(request.getCompany());
        interview.setPosition(request.getPosition());
        interview.setRound(request.getRound());
        interview.setInterviewDate(request.getInterviewDate());
        interview.setSummary(request.getSummary());
    }

    /** 只写入可变更字段；questionId 由添加时单独赋值 */
    private void apply(InterviewQuestion record, InterviewQuestionSaveRequest request) {
        record.setQuestionSnapshot(request.getQuestionSnapshot());
        record.setScore(request.getScore());
        record.setAnswered(request.getAnswered());
        record.setDeduction(request.getDeduction());
        record.setMyAnswerSnapshot(request.getMyAnswerSnapshot());
    }

    private Interview requireInterview(Long id) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "面试场次不存在");
        }
        return interview;
    }

    private InterviewQuestion requireRecord(Long interviewId, Long recordId) {
        InterviewQuestion record = interviewQuestionMapper.selectById(recordId);
        if (record == null || !record.getInterviewId().equals(interviewId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "面试题目记录不存在");
        }
        return record;
    }

    private List<InterviewQuestion> listRecords(Long interviewId) {
        return interviewQuestionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getInterviewId, interviewId));
    }

    /** 批量换算场次 VO：一次查询补齐题目数与平均分 */
    private List<InterviewVO> toVOs(List<Interview> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(Interview::getId).toList();
        Map<Long, List<InterviewQuestion>> byInterview = interviewQuestionMapper.selectList(
                        new LambdaQueryWrapper<InterviewQuestion>().in(InterviewQuestion::getInterviewId, ids))
                .stream()
                .collect(Collectors.groupingBy(InterviewQuestion::getInterviewId));
        return list.stream()
                .map(i -> toVO(i, byInterview.getOrDefault(i.getId(), List.of())))
                .toList();
    }

    private InterviewVO toVO(Interview i, List<InterviewQuestion> records) {
        InterviewVO vo = new InterviewVO();
        vo.setId(i.getId());
        vo.setCompany(i.getCompany());
        vo.setPosition(i.getPosition());
        vo.setRound(i.getRound());
        vo.setInterviewDate(i.getInterviewDate());
        vo.setSummary(i.getSummary());
        vo.setCreatedAt(i.getCreatedAt());
        vo.setQuestionCount(records.size());
        boolean hasScore = records.stream().anyMatch(q -> q.getScore() != null);
        if (hasScore) {
            double avg = records.stream()
                    .map(InterviewQuestion::getScore)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);
            vo.setAvgScore(Math.round(avg * 10) / 10.0);
        }
        return vo;
    }

    /** 批量换算题目记录 VO：一次查询补齐关联题目标题 */
    private List<InterviewQuestionVO> toQuestionVOs(List<InterviewQuestion> records) {
        Map<Long, String> titleMap = titleMap(records);
        return records.stream().map(q -> toQuestionVO(q, titleMap.get(q.getQuestionId()))).toList();
    }

    private Map<Long, String> titleMap(List<InterviewQuestion> records) {
        List<Long> questionIds = records.stream()
                .map(InterviewQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Question::getTitle));
    }

    private String titleOf(Long questionId) {
        if (questionId == null) {
            return null;
        }
        Question question = questionMapper.selectById(questionId);
        return question == null ? null : question.getTitle();
    }

    private InterviewQuestionVO toQuestionVO(InterviewQuestion q, String questionTitle) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setId(q.getId());
        vo.setInterviewId(q.getInterviewId());
        vo.setQuestionId(q.getQuestionId());
        vo.setQuestionTitle(questionTitle);
        vo.setQuestionSnapshot(q.getQuestionSnapshot());
        vo.setScore(q.getScore());
        vo.setAnswered(q.getAnswered());
        vo.setAnsweredLabel(answeredLabel(q.getAnswered()));
        vo.setDeduction(q.getDeduction());
        vo.setMyAnswerSnapshot(q.getMyAnswerSnapshot());
        return vo;
    }

    public static String answeredLabel(Integer answered) {
        return switch (answered == null ? 0 : answered) {
            case ANSWERED_FULL -> "答上";
            case ANSWERED_PARTIAL -> "部分答上";
            case ANSWERED_NOT -> "未答上";
            default -> "";
        };
    }
}
