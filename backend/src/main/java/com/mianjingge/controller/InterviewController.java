package com.mianjingge.controller;

import com.mianjingge.common.PageResult;
import com.mianjingge.common.Result;
import com.mianjingge.dto.InterviewQueryRequest;
import com.mianjingge.dto.InterviewQuestionSaveRequest;
import com.mianjingge.dto.InterviewSaveRequest;
import com.mianjingge.service.InterviewService;
import com.mianjingge.vo.InterviewDetailVO;
import com.mianjingge.vo.InterviewQuestionVO;
import com.mianjingge.vo.InterviewVO;
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

@Tag(name = "面试记录")
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Validated
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "分页查询面试场次（关键词匹配公司/岗位/轮次）")
    @GetMapping
    public Result<PageResult<InterviewVO>> page(@Valid InterviewQueryRequest request) {
        return Result.ok(interviewService.page(request));
    }

    @Operation(summary = "面试场次详情（含题目记录）")
    @GetMapping("/{id}")
    public Result<InterviewDetailVO> detail(@PathVariable Long id) {
        return Result.ok(interviewService.detail(id));
    }

    @Operation(summary = "新建面试场次")
    @PostMapping
    public Result<InterviewVO> create(@Valid @RequestBody InterviewSaveRequest request) {
        return Result.ok(interviewService.create(request));
    }

    @Operation(summary = "更新面试场次")
    @PutMapping("/{id}")
    public Result<InterviewVO> update(@PathVariable Long id, @Valid @RequestBody InterviewSaveRequest request) {
        return Result.ok(interviewService.update(id, request));
    }

    @Operation(summary = "删除面试场次（级联删除题目记录，不影响题库）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        interviewService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "向场次添加题目记录（关联题库或现场新题）")
    @PostMapping("/{interviewId}/questions")
    public Result<InterviewQuestionVO> addQuestion(@PathVariable Long interviewId,
                                                   @Valid @RequestBody InterviewQuestionSaveRequest request) {
        return Result.ok(interviewService.addQuestion(interviewId, request));
    }

    @Operation(summary = "更新题目记录（得分/是否答上/扣分点/现场回答）")
    @PutMapping("/{interviewId}/questions/{recordId}")
    public Result<InterviewQuestionVO> updateQuestion(@PathVariable Long interviewId,
                                                      @PathVariable Long recordId,
                                                      @Valid @RequestBody InterviewQuestionSaveRequest request) {
        return Result.ok(interviewService.updateQuestion(interviewId, recordId, request));
    }

    @Operation(summary = "删除题目记录")
    @DeleteMapping("/{interviewId}/questions/{recordId}")
    public Result<Void> deleteQuestion(@PathVariable Long interviewId, @PathVariable Long recordId) {
        interviewService.deleteQuestion(interviewId, recordId);
        return Result.ok();
    }

    @Operation(summary = "把现场回答保存为题库个人话术")
    @PostMapping("/{interviewId}/questions/{recordId}/save-as-phrase")
    public Result<Void> saveAsPhrase(@PathVariable Long interviewId, @PathVariable Long recordId) {
        interviewService.saveAsPhrase(interviewId, recordId);
        return Result.ok();
    }
}
