package com.mianjingge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 导入题目项：parse 响应与 confirm 请求共用 */
@Data
public class ImportItem {

    @NotBlank(message = "题目不能为空")
    @Size(max = 500, message = "题目过长")
    private String title;

    /** QuestionCategory 枚举名（预览可改） */
    @NotBlank(message = "分类不能为空")
    private String category;

    /** 1-3，默认 2 */
    @Min(value = 1, message = "难度取值 1-3")
    @Max(value = 3, message = "难度取值 1-3")
    private Integer difficulty;

    /** 0-10，未答为空 */
    @Min(value = 0, message = "得分取值 0-10")
    @Max(value = 10, message = "得分取值 0-10")
    private Integer score;

    /** 1 答上 2 部分答上 3 未答上 */
    @NotNull(message = "是否答上不能为空")
    @Min(value = 1, message = "是否答上取值 1-3")
    @Max(value = 3, message = "是否答上取值 1-3")
    private Integer answered;

    private String deduction;

    private String myAnswerSnapshot;

    private String stdAnswer;

    /** 标题全等匹配到的题库已有题 id，null 表示新建 */
    private Long matchedQuestionId;

    /** 复用已有题时是否用导入的参考答案覆盖其标准答案 */
    private Boolean overwriteStdAnswer;
}
