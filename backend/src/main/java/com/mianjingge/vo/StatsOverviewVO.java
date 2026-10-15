package com.mianjingge.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class StatsOverviewVO {

    /** 高频题 TOP10（按面试被问次数，仅关联题库的题） */
    private List<TopQuestionVO> topQuestions;

    /** 分类均分雷达（十大类全量，无数据类 avgScore 为空） */
    private List<CategoryScoreVO> categoryRadar;

    /** 场次均分折线（按面试日期升序） */
    private List<InterviewTrendVO> interviewTrend;

    @Data
    public static class TopQuestionVO {
        private Long questionId;
        private String title;
        private String category;
        private String categoryLabel;
        /** 被面试问到次数 */
        private Integer askedCount;
        /** 该题历次面试平均得分（未打分不参与） */
        private Double avgScore;
    }

    @Data
    public static class CategoryScoreVO {
        private String category;
        private String categoryLabel;
        /** 该分类关联题目的平均得分，无数据为空 */
        private Double avgScore;
        /** 该分类参与统计的得分记录数 */
        private Integer scoreCount;
        /** 该分类被问到的题目数（去重） */
        private Integer questionCount;
    }

    @Data
    public static class InterviewTrendVO {
        private Long interviewId;
        private String company;
        private String position;
        private String round;
        private LocalDate interviewDate;
        /** 该场平均分（无打分题时为空） */
        private Double avgScore;
        private Integer questionCount;
    }
}
