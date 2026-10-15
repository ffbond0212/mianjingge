package com.mianjingge.controller;

import com.mianjingge.common.Result;
import com.mianjingge.dto.ApplicationSaveRequest;
import com.mianjingge.dto.ApplicationTransitionRequest;
import com.mianjingge.service.ApplicationService;
import com.mianjingge.vo.ApplicationTimelineVO;
import com.mianjingge.vo.ApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "投递看板")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "看板全量投递列表")
    @GetMapping
    public Result<List<ApplicationVO>> list() {
        return Result.ok(applicationService.list());
    }

    @Operation(summary = "新建投递（初始状态 已投递）")
    @PostMapping
    public Result<ApplicationVO> create(@RequestBody @Valid ApplicationSaveRequest request) {
        return Result.ok(applicationService.create(request));
    }

    @Operation(summary = "编辑投递基本信息")
    @PutMapping("/{id}")
    public Result<ApplicationVO> update(@PathVariable Long id, @RequestBody @Valid ApplicationSaveRequest request) {
        return Result.ok(applicationService.update(id, request));
    }

    @Operation(summary = "删除投递（级联删时间线）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "状态流转：按主流程推进一步或标记已挂，可关联面试场次")
    @PostMapping("/{id}/transition")
    public Result<ApplicationVO> transition(@PathVariable Long id, @RequestBody @Valid ApplicationTransitionRequest request) {
        return Result.ok(applicationService.transition(id, request));
    }

    @Operation(summary = "该投递的状态时间线")
    @GetMapping("/{id}/timeline")
    public Result<List<ApplicationTimelineVO>> timeline(@PathVariable Long id) {
        return Result.ok(applicationService.timeline(id));
    }
}
