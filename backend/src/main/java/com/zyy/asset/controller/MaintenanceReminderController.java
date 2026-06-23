package com.zyy.asset.controller;

import com.zyy.asset.model.vo.EquipmentVO;
import com.zyy.asset.service.MaintenanceReminderService;
import com.zyy.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for maintenance reminder operations.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET  /api/equipment/maintenance/overdue  - List overdue equipment</li>
 *   <li>GET  /api/equipment/maintenance/upcoming - List upcoming maintenance</li>
 *   <li>POST /api/equipment/maintenance/check    - Trigger manual check (broadcasts alerts)</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance Reminder", description = "Equipment maintenance schedule monitoring and alerting")
public class MaintenanceReminderController {

    private final MaintenanceReminderService maintenanceReminderService;

    @GetMapping("/overdue")
    @Operation(summary = "Overdue maintenance", description = "List equipment with overdue maintenance schedules")
    public Result<List<EquipmentVO>> getOverdueMaintenance() {
        List<EquipmentVO> list = maintenanceReminderService.getOverdueMaintenance();
        return Result.ok(list);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Upcoming maintenance", description = "List equipment due for maintenance within N days")
    public Result<List<EquipmentVO>> getUpcomingMaintenance(
            @Parameter(description = "Days to look ahead (default 7)") @RequestParam(defaultValue = "7") int days) {
        List<EquipmentVO> list = maintenanceReminderService.getUpcomingMaintenance(days);
        return Result.ok(list);
    }

    @PostMapping("/check")
    @Operation(summary = "Trigger maintenance check", description = "Manually trigger a maintenance check and broadcast alerts via WebSocket")
    public Result<Void> checkAndAlert() {
        maintenanceReminderService.checkAndAlert();
        return Result.ok(null, "Maintenance check completed, alerts broadcast");
    }
}
