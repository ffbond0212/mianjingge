package com.mianjingge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianjingge.entity.Interview;
import com.mianjingge.entity.InterviewQuestion;
import com.mianjingge.entity.Question;
import com.mianjingge.enums.QuestionCategory;
import com.mianjingge.mapper.InterviewMapper;
import com.mianjingge.mapper.InterviewQuestionMapper;
import com.mianjingge.mapper.QuestionMapper;
import com.mianjingge.vo.StatsOverviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    public static final String CACHE_KEY = "mjg:stats:overview";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionMapper questionMapper;
    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;

    /** 复盘统计总览：先读缓存，未命中则计算并写入（30 分钟 TTL） */
    public StatsOverviewVO overview() {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, StatsOverviewVO.class);
            } catch (Exception e) {
                log.warn("统计缓存反序列化失败，重算: {}", e.getMessage());
            }
        }
        StatsOverviewVO vo = compute();
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(vo), CACHE_TTL);
        } catch (Exception e) {
            log.warn("统计缓存写入失败（Redis 不可用，返回实时计算）: {}", e.getMessage());
        }
        return vo;
    }

    /** 写时失效：题库/面试记录变更后调用 */
    public void invalidate() {
        try {
            redisTemplate.delete(CACHE_KEY);
        } catch (Exception e) {
            log.warn("统计缓存删除失败（Redis 不可用）: {}", e.getMessage());
        }
    }

    /** 个人工具数据量小，全表读出后在内存聚合 */
    private StatsOverviewVO compute() {
        List<Question> questions = questionMapper.selectList(null);
        List<Interview> interviews = interviewMapper.selectList(null);
        List<InterviewQuestion> records = interviewQuestionMapper.selectList(null);

        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTopQuestions(computeTopQuestions(records, questionMap));
        vo.setCategoryRadar(computeCategoryRadar(records, questionMap));
        vo.setInterviewTrend(computeTrend(interviews, records));
        return vo;
    }

    private List<StatsOverviewVO.TopQuestionVO> computeTopQuestions(List<InterviewQuestion> records,
                                                                    Map<Long, Question> questionMap) {
        // 按关联题分组：被问次数 + 平均得分
        Map<Long, List<InterviewQuestion>> byQuestion = records.stream()
                .filter(r -> r.getQuestionId() != null)
                .collect(Collectors.groupingBy(InterviewQuestion::getQuestionId));
        return byQuestion.entrySet().stream()
                .map(e -> {
                    Question q = questionMap.get(e.getKey());
                    StatsOverviewVO.TopQuestionVO vo = new StatsOverviewVO.TopQuestionVO();
                    vo.setQuestionId(e.getKey());
                    vo.setTitle(q != null ? q.getTitle() : "(已删除题目)");
                    vo.setCategory(q != null ? q.getCategory().name() : null);
                    vo.setCategoryLabel(q != null ? q.getCategory().getLabel() : null);
                    vo.setAskedCount(e.getValue().size());
                    vo.setAvgScore(avgScore(e.getValue()));
                    return vo;
                })
                .sorted(Comparator.comparingInt(StatsOverviewVO.TopQuestionVO::getAskedCount).reversed())
                .limit(10)
                .toList();
    }

    private List<StatsOverviewVO.CategoryScoreVO> computeCategoryRadar(List<InterviewQuestion> records,
                                                                       Map<Long, Question> questionMap) {
        Map<Long, QuestionCategory> categoryOf = questionMap.values().stream()
                .collect(Collectors.toMap(Question::getId, Question::getCategory));
        // 十大类全量输出，保证雷达轴固定
        Map<QuestionCategory, List<InterviewQuestion>> byCategory = records.stream()
                .filter(r -> r.getQuestionId() != null && categoryOf.containsKey(r.getQuestionId()))
                .collect(Collectors.groupingBy(r -> categoryOf.get(r.getQuestionId())));

        List<StatsOverviewVO.CategoryScoreVO> list = new ArrayList<>();
        for (QuestionCategory category : QuestionCategory.values()) {
            List<InterviewQuestion> group = byCategory.getOrDefault(category, List.of());
            StatsOverviewVO.CategoryScoreVO vo = new StatsOverviewVO.CategoryScoreVO();
            vo.setCategory(category.name());
            vo.setCategoryLabel(category.getLabel());
            long scored = group.stream().filter(r -> r.getScore() != null).count();
            vo.setScoreCount((int) scored);
            vo.setAvgScore(scored > 0 ? avgScore(group) : null);
            // 去重题目数
            vo.setQuestionCount((int) group.stream().map(InterviewQuestion::getQuestionId).distinct().count());
            list.add(vo);
        }
        return list;
    }

    private List<StatsOverviewVO.InterviewTrendVO> computeTrend(List<Interview> interviews,
                                                                List<InterviewQuestion> records) {
        Map<Long, List<InterviewQuestion>> byInterview = records.stream()
                .collect(Collectors.groupingBy(InterviewQuestion::getInterviewId));
        return interviews.stream()
                .sorted(Comparator.comparing(Interview::getInterviewDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Interview::getId))
                .map(i -> {
                    List<InterviewQuestion> group = byInterview.getOrDefault(i.getId(), List.of());
                    StatsOverviewVO.InterviewTrendVO vo = new StatsOverviewVO.InterviewTrendVO();
                    vo.setInterviewId(i.getId());
                    vo.setCompany(i.getCompany());
                    vo.setPosition(i.getPosition());
                    vo.setRound(i.getRound());
                    vo.setInterviewDate(i.getInterviewDate());
                    long scored = group.stream().filter(r -> r.getScore() != null).count();
                    vo.setAvgScore(scored > 0 ? avgScore(group) : null);
                    vo.setQuestionCount(group.size());
                    return vo;
                })
                .toList();
    }

    /** 平均分（只统计有分的记录），无分返回 null，保留 1 位小数 */
    private Double avgScore(List<InterviewQuestion> records) {
        double avg = records.stream()
                .map(InterviewQuestion::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);
        return Double.isNaN(avg) ? null : Math.round(avg * 10) / 10.0;
    }
}
