package com.zyy.controller;

import com.zyy.common.PageParam;
import com.zyy.common.Result;
import com.zyy.model.dto.EquipmentSaveDTO;
import com.zyy.model.dto.EquipmentUpdateDTO;
import com.zyy.model.vo.EquipmentVO;
import com.zyy.model.vo.PageVO;
import com.zyy.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import com.zyy.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for equipment lifecycle management.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET    /api/equipment          - Paginated list with filters</li>
 *   <li>GET    /api/equipment/{id}     - Single equipment record</li>
 *   <li>POST   /api/equipment          - Register new equipment</li>
 *   <li>PUT    /api/equipment/{id}     - Update equipment details</li>
 *   <li>PATCH  /api/equipment/{id}/status - Status transition</li>
 *   <li>DELETE /api/equipment/{id}    - Soft delete</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment Management", description = "Equipment asset lifecycle and maintenance management")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final SecurityUtils securityUtils;

    /**
     * Register a new equipment asset.
     */
    @PostMapping
    @PreAuthorize("@ss.hasAuthority('equipment:add')")
    @Operation(summary = "Register equipment", description = "Create a new equipment asset record")
    public Result<EquipmentVO> save(
            @Valid @RequestBody EquipmentSaveDTO saveDTO) {
        Long operatorId = getCurrentUserId();
        EquipmentVO vo = equipmentService.save(saveDTO, operatorId);
        return Result.ok(vo, "Equipment registered successfully");
    }

    /**
     * Update an existing equipment record.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('equipment:edit')")
    @Operation(summary = "Update equipment", description = "Update equipment details (not for status changes)")
    public Result<EquipmentVO> update(
            @Parameter(description = "Equipment ID") @PathVariable Long id,
            @Valid @RequestBody EquipmentUpdateDTO updateDTO) {
        Long operatorId = getCurrentUserId();
        updateDTO.setId(id);
        EquipmentVO vo = equipmentService.update(updateDTO, operatorId);
        return Result.ok(vo, "Equipment updated successfully");
    }

    /**
     * Get a single equipment record by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get equipment by ID")
    @PreAuthorize("@ss.hasAuthority('equipment:detail')")
    public Result<EquipmentVO> getById(
            @Parameter(description = "Equipment ID") @PathVariable Long id) {
        EquipmentVO vo = equipmentService.getById(id);
        return Result.ok(vo);
    }

    /**
     * Get paginated equipment list with optional filters.
     */
    @GetMapping
    @Operation(summary = "List equipment", description = "Paginated equipment list with optional filters")
    @PreAuthorize("@ss.hasAuthority('equipment:list')")
    public Result<PageVO<EquipmentVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Equipment name filter (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Category filter (exact match)") @RequestParam(required = false) String category,
            @Parameter(description = "Status filter: 0=maintenance, 1=normal, 2=scrapped") @RequestParam(required = false) Integer status) {
        PageVO<EquipmentVO> page = equipmentService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), name, category, status);
        return Result.ok(page);
    }

    /**
     * Transition equipment status.
     * Validates state machine rules before applying the transition.
    @PreAuthorize("@ss.hasAuthority('equipment:edit')")
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update equipment status", description = "Change operational status with state machine validation")
    public Result<Void> updateStatus(
            @Parameter(description = "Equipment ID") @PathVariable Long id,
            @Parameter(description = "Target status: 0=maintenance, 1=normal, 2=scrapped") @RequestParam Integer status) {
        Long operatorId = getCurrentUserId();
        equipmentService.updateStatus(id, status, operatorId);
        return Result.ok(null, "Equipment status updated");
    }

    /**
     * Delete an equipment record (soft delete).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete equipment", description = "Soft delete an equipment record")
    @PreAuthorize("@ss.hasAuthority('equipment:del')")
    public Result<Void> delete(
            @Parameter(description = "Equipment ID") @PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        equipmentService.delete(id, operatorId);
        return Result.ok(null, "Equipment deleted");
    }

    private Long getCurrentUserId() {
        return securityUtils.currentUserId();
    }
}
