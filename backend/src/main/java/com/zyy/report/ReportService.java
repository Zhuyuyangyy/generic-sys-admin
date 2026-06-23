package com.zyy.report;

import com.zyy.report.model.AssetSummaryReport;
import com.zyy.report.model.AuditTrailReport;
import com.zyy.report.model.InventorySummaryReport;
import com.zyy.report.model.MaintenanceHistoryReport;
import com.zyy.report.model.StockMovementReport;

import java.time.LocalDateTime;

/**
 * Report generation business service interface.
 */
public interface ReportService {

    /**
     * Generate asset inventory summary report.
     *
     * @return asset summary report
     */
    AssetSummaryReport generateAssetSummary();

    /**
     * Generate consumable inventory summary report.
     *
     * @return inventory summary report
     */
    InventorySummaryReport generateInventorySummary();

    /**
     * Generate maintenance history report.
     *
     * @param startTime   start of date range
     * @param endTime     end of date range
     * @param equipmentId optional equipment filter
     * @return maintenance history report
     */
    MaintenanceHistoryReport generateMaintenanceHistory(LocalDateTime startTime, LocalDateTime endTime, Long equipmentId);

    /**
     * Generate audit trail report.
     *
     * @param startTime start of date range
     * @param endTime   end of date range
     * @param module    optional module filter
     * @param userId    optional user filter
     * @return audit trail report
     */
    AuditTrailReport generateAuditTrail(LocalDateTime startTime, LocalDateTime endTime, String module, Long userId);

    /**
     * Generate stock movement report.
     *
     * @param startTime    start of date range
     * @param endTime      end of date range
     * @param consumableId optional consumable filter
     * @return stock movement report
     */
    StockMovementReport generateStockMovement(LocalDateTime startTime, LocalDateTime endTime, Long consumableId);
}
