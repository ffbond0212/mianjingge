package com.mianjingge.controller;

import com.mianjingge.common.Result;
import com.mianjingge.dto.ReviewCheckinRequest;
import com.mianjingge.dto.ReviewIntervalsRequest;
import com.mianjingge.service.ReviewService;
import com.mianjingge.vo.ReviewPlanVO;
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
import java.util.Map;

@Tag(name = "复习计划")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "今日到期复习任务（全部到期，Redis ZSet 按下次复习时间排序）")
    @GetMapping("/today")
    public Result<List<ReviewPlanVO>> today() {
        return Result.ok(reviewService.today());
    }

    @Operation(summary = "全部进行中的复习计划")
    @GetMapping("/plans")
    public Result<List<ReviewPlanVO>> plans() {
        return Result.ok(reviewService.listPlans());
    }

    @Operation(summary = "手动入队")
    @PostMapping("/plans")
    public Result<ReviewPlanVO> enqueue(@RequestBody Map<String, Long> body) {
        return Result.ok(reviewService.enqueue(body.get("questionId")));
    }

    @Operation(summary = "打卡：答上进档×1.5 / 答差降档，返回新排期")
    @PostMapping("/plans/{id}/checkin")
    public Result<ReviewPlanVO> checkin(@PathVariable Long id, @RequestBody @Valid ReviewCheckinRequest request) {
        return Result.ok(reviewService.checkin(id, request));
    }

    @Operation(summary = "移出复习队列")
    @DeleteMapping("/plans/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        reviewService.removePlan(id);
        return Result.ok();
    }

    @Operation(summary = "读取遗忘曲线间隔配置")
    @GetMapping("/config")
    public Result<Map<String, List<Integer>>> config() {
        return Result.ok(Map.of("intervals", reviewService.getIntervals()));
    }

    @Operation(summary = "修改遗忘曲线间隔配置（全局，2-10 档递增）")
    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody @Valid ReviewIntervalsRequest request) {
        reviewService.updateIntervals(request.getIntervals());
        return Result.ok();
    }
}
