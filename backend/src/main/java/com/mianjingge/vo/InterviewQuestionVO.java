package com.mianjingge.vo;

import lombok.Data;

@Data
public class InterviewQuestionVO {

    private Long id;

    private Long interviewId;

    /** 关联题库题目 id，未关联为空 */
    private Long questionId;

    /** 关联题库题目的标题，未关联为空 */
    private String questionTitle;

    /** 现场题目原文快照 */
    private String questionSnapshot;

    /** 得分 0-10，未答为空 */
    private Integer score;

    /** 1 答上 2 部分答上 3 未答上 */
    private Integer answered;

    private String answeredLabel;

    /** 扣分点 */
    private String deduction;

    /** 现场回答快照 */
    private String myAnswerSnapshot;
}
