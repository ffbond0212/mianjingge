package com.mianjingge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionSaveRequest {

    @NotBlank(message = "题目不能为空")
    @Size(max = 500, message = "题目过长")
    private String title;

    @NotBlank(message = "分类不能为空")
    private String category;

    @Size(max = 255, message = "标签过长")
    private String tags;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度取值 1-3")
    @Max(value = 3, message = "难度取值 1-3")
    private Integer difficulty;

    private String stdAnswer;

    private String myAnswer;
}
