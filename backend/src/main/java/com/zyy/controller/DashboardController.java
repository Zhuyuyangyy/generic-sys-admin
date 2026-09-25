package com.zyy.controller;

import com.zyy.common.Result;
import com.zyy.model.vo.DashboardStatsVO;
import com.zyy.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard 统计聚合接口
 * 提供系统各模块的统计聚合数据，禁止前端拉全量明细
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "系统首页统计聚合")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取首页统计数据
     * 聚合设备、耗材、用户、日志趋势等多维度数据
     */
    @GetMapping("/stats")
    @Operation(summary = "首页统计聚合", description = "返回设备/耗材/用户统计及操作日志趋势")
    @PreAuthorize("@ss.hasAuthority('dashboard:view')")
    public Result<DashboardStatsVO> getStats() {
        log.debug("Dashboard stats requested");
        return Result.ok(dashboardService.getStats());
    }
}