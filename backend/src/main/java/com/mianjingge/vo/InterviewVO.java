package com.mianjingge.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InterviewVO {

    private Long id;

    private String company;

    private String position;

    private String round;

    private LocalDate interviewDate;

    private String summary;

    /** 题目记录数 */
    private Integer questionCount;

    /** 平均分（只统计已打分的题），未打分时为空 */
    private Double avgScore;

    private LocalDateTime createdAt;
}
