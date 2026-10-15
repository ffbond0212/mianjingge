package com.mianjingge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_question")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long interviewId;

    /** 关联题库题目 id，现场新题未关联时为空 */
    private Long questionId;

    /** 现场题目原文快照 */
    private String questionSnapshot;

    /** 得分 0-10，未答为空 */
    private Integer score;

    /** 1 答上 2 部分答上 3 未答上 */
    private Integer answered;

    /** 扣分点 */
    private String deduction;

    /** 现场回答快照 */
    private String myAnswerSnapshot;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
