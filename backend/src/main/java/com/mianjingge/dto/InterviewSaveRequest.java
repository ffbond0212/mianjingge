package com.mianjingge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InterviewSaveRequest {

    @NotBlank(message = "公司不能为空")
    @Size(max = 100, message = "公司名过长")
    private String company;

    @NotBlank(message = "岗位不能为空")
    @Size(max = 100, message = "岗位名过长")
    private String position;

    @Size(max = 50, message = "轮次过长")
    private String round;

    /** 面试日期 yyyy-MM-dd */
    private LocalDate interviewDate;

    /** 整体总结 */
    private String summary;
}
