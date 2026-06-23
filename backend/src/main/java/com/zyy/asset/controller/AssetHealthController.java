package com.zyy.asset.controller;

import com.zyy.common.Result;
import com.zyy.asset.model.vo.AssetHealthOverview;
import com.zyy.asset.model.vo.AssetHealthScore;
import com.zyy.asset.service.AssetHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for asset health scoring and monitoring.
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment/health")
@RequiredArgsConstructor
@Tag(name = "Asset Health Scoring", description = "Equipment health score calculation and monitoring APIs")
public class AssetHealthController {

    private final AssetHealthService assetHealthService;

    @GetMapping
    @Operation(summary = "Get all equipment health scores", description = "Calculate and return health scores for all active equipment")
    public Result<List<AssetHealthScore>> getAllHealthScores() {
        List<AssetHealthScore> scores = assetHealthService.getAllHealthScores();
        return Result.ok(scores);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single equipment health score", description = "Calculate and return health score for a specific equipment")
    public Result<AssetHealthScore> getHealthScore(
            @Parameter(description = "Equipment ID") @PathVariable Long id) {
        AssetHealthScore score = assetHealthService.calculateHealthScore(id);
        return Result.ok(score);
    }

    @GetMapping("/overview")
    @Operation(summary = "Get health overview", description = "Get an overview summary of all equipment health")
    public Result<AssetHealthOverview> getHealthOverview() {
        AssetHealthOverview overview = assetHealthService.getHealthOverview();
        return Result.ok(overview);
    }
}
