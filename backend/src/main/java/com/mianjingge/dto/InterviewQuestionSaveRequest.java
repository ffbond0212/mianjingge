package com.mianjingge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InterviewQuestionSaveRequest {

    /** 关联题库题目 id，现场新题未关联时为空 */
    private Long questionId;

    @NotBlank(message = "题目快照不能为空")
    @Size(max = 500, message = "题目快照过长")
    private String questionSnapshot;

    /** 得分 0-10，未答不填 */
    @Min(value = 0, message = "得分取值 0-10")
    @Max(value = 10, message = "得分取值 0-10")
    private Integer score;

    /** 1 答上 2 部分答上 3 未答上 */
    @NotNull(message = "是否答上不能为空")
    @Min(value = 1, message = "是否答上取值 1-3")
    @Max(value = 3, message = "是否答上取值 1-3")
    private Integer answered;

    /** 扣分点 */
    private String deduction;

    /** 现场回答快照 */
    private String myAnswerSnapshot;
}
