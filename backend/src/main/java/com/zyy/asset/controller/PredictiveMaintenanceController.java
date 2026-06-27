package com.zyy.asset.controller;

import com.zyy.common.Result;
import com.zyy.asset.model.vo.MaintenancePredictionVO;
import com.zyy.asset.service.PredictiveMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for predictive maintenance endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Predictive Maintenance", description = "Equipment maintenance prediction and risk assessment APIs")
public class PredictiveMaintenanceController {

    private final PredictiveMaintenanceService predictiveMaintenanceService;

    @GetMapping("/{id}/maintenance-prediction")
    @Operation(summary = "Get maintenance prediction", description = "Predict next maintenance date and assess risk for a specific equipment")
    public Result<MaintenancePredictionVO> getMaintenancePrediction(
            @Parameter(description = "Equipment ID") @PathVariable Long id) {
        MaintenancePredictionVO prediction = predictiveMaintenanceService.predictNextMaintenance(id);
        return Result.ok(prediction);
    }

    @GetMapping("/maintenance-predictions")
    @Operation(summary = "Get all maintenance predictions", description = "Predict next maintenance dates and assess risks for all active equipment")
    public Result<List<MaintenancePredictionVO>> getAllMaintenancePredictions() {
        List<MaintenancePredictionVO> predictions = predictiveMaintenanceService.getAllMaintenancePredictions();
        return Result.ok(predictions);
    }
}
