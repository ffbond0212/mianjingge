package com.mianjingge.controller;

import com.mianjingge.common.PageResult;
import com.mianjingge.common.Result;
import com.mianjingge.dto.QuestionQueryRequest;
import com.mianjingge.dto.QuestionSaveRequest;
import com.mianjingge.service.QuestionService;
import com.mianjingge.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "题库管理")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Validated
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "分页查询题目（关键词/分类/来源/难度过滤）")
    @GetMapping
    public Result<PageResult<QuestionVO>> page(@Valid QuestionQueryRequest request) {
        return Result.ok(questionService.page(request));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public Result<QuestionVO> get(@PathVariable Long id) {
        return Result.ok(questionService.getById(id));
    }

    @Operation(summary = "新增题目（手动录入）")
    @PostMapping
    public Result<QuestionVO> create(@Valid @RequestBody QuestionSaveRequest request) {
        return Result.ok(questionService.create(request));
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    public Result<QuestionVO> update(@PathVariable Long id, @Valid @RequestBody QuestionSaveRequest request) {
        return Result.ok(questionService.update(id, request));
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }
}
