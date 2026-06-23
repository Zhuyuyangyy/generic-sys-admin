package com.zyy.report;

import com.zyy.common.Result;
import com.zyy.report.model.AssetSummaryReport;
import com.zyy.report.model.AuditTrailReport;
import com.zyy.report.model.InventorySummaryReport;
import com.zyy.report.model.MaintenanceHistoryReport;
import com.zyy.report.model.StockMovementReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * RESTful controller for report generation.
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report Generation", description = "Report generation and data aggregation APIs")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/asset-summary")
    @Operation(summary = "Asset inventory summary", description = "Generate a summary report of all equipment assets")
    public Result<AssetSummaryReport> getAssetSummary() {
        AssetSummaryReport report = reportService.generateAssetSummary();
        return Result.ok(report);
    }

    @GetMapping("/inventory-summary")
    @Operation(summary = "Consumable inventory summary", description = "Generate a summary report of consumable inventory")
    public Result<InventorySummaryReport> getInventorySummary() {
        InventorySummaryReport report = reportService.generateInventorySummary();
        return Result.ok(report);
    }

    @GetMapping("/maintenance-history")
    @Operation(summary = "Maintenance history report", description = "Generate maintenance history report with optional filters")
    public Result<MaintenanceHistoryReport> getMaintenanceHistory(
            @Parameter(description = "Start time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "Equipment ID filter") @RequestParam(required = false) Long equipmentId) {
        MaintenanceHistoryReport report = reportService.generateMaintenanceHistory(startTime, endTime, equipmentId);
        return Result.ok(report);
    }

    @GetMapping("/audit-trail")
    @Operation(summary = "Audit trail report", description = "Generate audit trail report with optional filters")
    public Result<AuditTrailReport> getAuditTrail(
            @Parameter(description = "Start time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "Module filter (partial match)") @RequestParam(required = false) String module,
            @Parameter(description = "User ID filter") @RequestParam(required = false) Long userId) {
        AuditTrailReport report = reportService.generateAuditTrail(startTime, endTime, module, userId);
        return Result.ok(report);
    }

    @GetMapping("/stock-movement")
    @Operation(summary = "Stock movement report", description = "Generate stock movement report with optional filters")
    public Result<StockMovementReport> getStockMovement(
            @Parameter(description = "Start time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "Consumable ID filter") @RequestParam(required = false) Long consumableId) {
        StockMovementReport report = reportService.generateStockMovement(startTime, endTime, consumableId);
        return Result.ok(report);
    }
}
