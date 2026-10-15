package com.mianjingge.controller;

import com.mianjingge.common.Result;
import com.mianjingge.service.StatsService;
import com.mianjingge.vo.StatsOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "复盘统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "复盘统计总览（高频题 TOP10 / 分类雷达 / 场次折线，Redis 缓存 30 分钟）")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(statsService.overview());
    }
}
