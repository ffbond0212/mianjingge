package com.mianjingge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplicationTransitionRequest {

    @NotBlank(message = "目标状态不能为空")
    private String toStatus;

    @Size(max = 200, message = "备注最长 200 字")
    private String remark;

    /** 可选：关联面试场次（笔试/面试阶段可关联复盘） */
    private Long interviewId;
}
