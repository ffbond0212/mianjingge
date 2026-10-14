package com.mianjingge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class QuestionQueryRequest {

    @Min(value = 1, message = "页码从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer size = 10;

    /** 关键词：模糊匹配 标题/标准答案/个人话术 */
    private String keyword;

    /** QuestionCategory 枚举名 */
    private String category;

    /** PRESET / IMPORT / MANUAL */
    private String source;

    /** 1-3 */
    private Integer difficulty;
}
