package com.mianjingge.controller;

import com.mianjingge.common.Result;
import com.mianjingge.dto.ImportConfirmRequest;
import com.mianjingge.dto.ImportParseRequest;
import com.mianjingge.dto.ImportParseResponse;
import com.mianjingge.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "md 导入")
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Validated
public class ImportController {

    private final ImportService importService;

    @Operation(summary = "解析面试记录 md，返回预览（场次信息 + 题目项，不落库）")
    @PostMapping("/md/parse")
    public Result<ImportParseResponse> parse(@Valid @RequestBody ImportParseRequest request) {
        return Result.ok(importService.parse(request.getContent()));
    }

    @Operation(summary = "确认导入：按预览数据落库（场次 + 题目 + 记录）")
    @PostMapping("/md/confirm")
    public Result<Map<String, Long>> confirm(@Valid @RequestBody ImportConfirmRequest request) {
        return Result.ok(Map.of("interviewId", importService.confirm(request)));
    }
}
