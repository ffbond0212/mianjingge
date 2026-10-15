package com.mianjingge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ImportConfirmRequest {

    @Valid
    @NotNull(message = "场次信息不能为空")
    private ImportInterviewInfo interview;

    @Valid
    @NotEmpty(message = "题目列表不能为空")
    private List<ImportItem> items;
}
