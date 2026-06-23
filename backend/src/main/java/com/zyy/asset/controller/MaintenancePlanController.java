package com.zyy.asset.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.asset.model.dto.MaintenancePlanSaveDTO;
import com.zyy.asset.model.dto.MaintenancePlanUpdateDTO;
import com.zyy.asset.model.vo.MaintenancePlanVO;
import com.zyy.asset.service.MaintenancePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for maintenance plan management.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET    /api/equipment/maintenance-plans          - Paginated list with filters</li>
 *   <li>GET    /api/equipment/maintenance-plans/{id}     - Single plan record</li>
 *   <li>POST   /api/equipment/maintenance-plans          - Create new plan</li>
 *   <li>PUT    /api/equipment/maintenance-plans/{id}     - Update plan details</li>
 *   <li>PATCH  /api/equipment/maintenance-plans/{id}/status - Status transition</li>
 *   <li>DELETE /api/equipment/maintenance-plans/{id}     - Soft delete</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment/maintenance-plans")
@RequiredArgsConstructor
@Tag(name = "Maintenance Plan Management", description = "Equipment maintenance plan lifecycle management")
public class MaintenancePlanController {

    private final MaintenancePlanService maintenancePlanService;

    @PostMapping
    @Operation(summary = "Create maintenance plan", description = "Create a new maintenance plan for equipment")
    public Result<MaintenancePlanVO> save(
            @Valid @RequestBody MaintenancePlanSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        MaintenancePlanVO vo = maintenancePlanService.save(saveDTO, operatorId);
        return Result.ok(vo, "Maintenance plan created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update maintenance plan", description = "Update maintenance plan details (not for status changes)")
    public Result<MaintenancePlanVO> update(
            @Parameter(description = "Plan ID") @PathVariable Long id,
            @Valid @RequestBody MaintenancePlanUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        MaintenancePlanVO vo = maintenancePlanService.update(updateDTO, operatorId);
        return Result.ok(vo, "Maintenance plan updated successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance plan by ID")
    public Result<MaintenancePlanVO> getById(
            @Parameter(description = "Plan ID") @PathVariable Long id) {
        MaintenancePlanVO vo = maintenancePlanService.getById(id);
        return Result.ok(vo);
    }

    @GetMapping
    @Operation(summary = "List maintenance plans", description = "Paginated maintenance plan list with optional filters")
    public Result<PageVO<MaintenancePlanVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Equipment ID filter") @RequestParam(required = false) Long equipmentId,
            @Parameter(description = "Status filter: PENDING, IN_PROGRESS, COMPLETED, CANCELLED") @RequestParam(required = false) String status) {
        PageVO<MaintenancePlanVO> page = maintenancePlanService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), equipmentId, status);
        return Result.ok(page);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update plan status", description = "Activate, complete, or cancel a maintenance plan")
    public Result<Void> updateStatus(
            @Parameter(description = "Plan ID") @PathVariable Long id,
            @Parameter(description = "Target status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED") @RequestParam String status,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        maintenancePlanService.updateStatus(id, status, operatorId);
        return Result.ok(null, "Maintenance plan status updated");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete maintenance plan", description = "Soft delete a maintenance plan")
    public Result<Void> delete(
            @Parameter(description = "Plan ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        maintenancePlanService.delete(id, operatorId);
        return Result.ok(null, "Maintenance plan deleted");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
