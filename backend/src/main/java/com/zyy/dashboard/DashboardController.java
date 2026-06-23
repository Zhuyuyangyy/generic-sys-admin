package com.zyy.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.zyy.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for dashboard statistics.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET /api/dashboard/overview           - Overview statistics</li>
 *   <li>GET /api/dashboard/equipment-status   - Equipment status distribution</li>
 *   <li>GET /api/dashboard/inventory-summary  - Inventory summary</li>
 *   <li>GET /api/dashboard/recent-activities  - Recent operation logs</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics and overview")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Overview statistics", description = "Get overview stats including equipment counts, consumable counts, and pending maintenance")
    public Result<DashboardOverview> getOverview() {
        DashboardOverview overview = dashboardService.getOverview();
        return Result.ok(overview);
    }

    @GetMapping("/equipment-status")
    @Operation(summary = "Equipment status distribution", description = "Get equipment status distribution with percentages")
    public Result<EquipmentStatusDistribution> getEquipmentStatusDistribution() {
        EquipmentStatusDistribution distribution = dashboardService.getEquipmentStatusDistribution();
        return Result.ok(distribution);
    }

    @GetMapping("/inventory-summary")
    @Operation(summary = "Inventory summary", description = "Get consumable inventory summary including stock alerts and total value")
    public Result<InventorySummary> getInventorySummary() {
        InventorySummary summary = dashboardService.getInventorySummary();
        return Result.ok(summary);
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "Recent activities", description = "Get recent operation logs across all modules")
    public Result<List<RecentActivity>> getRecentActivities(
            @Parameter(description = "Maximum number of activities to return") @RequestParam(defaultValue = "10") int limit) {
        List<RecentActivity> activities = dashboardService.getRecentActivities(limit);
        return Result.ok(activities);
    }
}
