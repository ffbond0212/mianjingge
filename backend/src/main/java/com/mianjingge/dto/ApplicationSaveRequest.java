package com.mianjingge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ApplicationSaveRequest {

    @NotBlank(message = "公司不能为空")
    @Size(max = 100, message = "公司最长 100 字")
    private String company;

    @NotBlank(message = "岗位不能为空")
    @Size(max = 100, message = "岗位最长 100 字")
    private String position;

    @Size(max = 50, message = "渠道最长 50 字")
    private String channel;

    private LocalDate applyDate;

    @Size(max = 50, message = "城市最长 50 字")
    private String city;

    @Size(max = 100, message = "薪资最长 100 字")
    private String salary;

    @Size(max = 500, message = "JD 链接最长 500 字")
    private String jdUrl;

    @Size(max = 500, message = "备注最长 500 字")
    private String remark;
}
