package com.mianjingge.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InterviewDetailVO {

    private InterviewVO interview;

    private List<InterviewQuestionVO> questions;
}
