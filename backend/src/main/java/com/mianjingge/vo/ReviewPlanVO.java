package com.mianjingge.vo;

import com.mianjingge.enums.QuestionCategory;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewPlanVO {

    private Long id;

    private Long questionId;

    /** 题目标题（题目已删除时回落到入队快照） */
    private String questionTitle;

    private QuestionCategory category;

    private String categoryLabel;

    /** 当前档位（COMPLETED 时为间隔档数） */
    private Integer stage;

    /** 第N轮 / 已毕业 */
    private String stageLabel;

    /** 当前间隔天数（COMPLETED 为 0） */
    private Integer intervalDays;

    private LocalDate nextReviewDate;

    private Integer reviewCount;

    private Integer lastScore;

    private String status;
}
