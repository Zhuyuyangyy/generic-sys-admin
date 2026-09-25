package com.zyy.controller;

import com.zyy.common.PageParam;
import com.zyy.common.Result;
import com.zyy.model.dto.InventoryRecordSaveDTO;
import com.zyy.model.dto.InventoryRecordUpdateDTO;
import com.zyy.model.vo.InventoryRecordVO;
import com.zyy.model.vo.PageVO;
import com.zyy.security.SecurityUtils;
import com.zyy.service.InventoryRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for equipment maintenance and inspection record management.
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory-records")
@RequiredArgsConstructor
@Tag(name = "Inventory Record Management", description = "Equipment maintenance and inspection lifecycle tracking")
public class InventoryRecordController {

    private final InventoryRecordService recordService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("@ss.hasAuthority('inventory:add')")
    @Operation(summary = "Create record", description = "Create a new equipment maintenance or inspection record")
    public Result<InventoryRecordVO> save(
            @Valid @RequestBody InventoryRecordSaveDTO saveDTO) {
        InventoryRecordVO vo = recordService.save(saveDTO, getCurrentUserId());
        return Result.ok(vo, "Record created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('inventory:edit')")
    @Operation(summary = "Update record")
    public Result<InventoryRecordVO> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRecordUpdateDTO updateDTO) {
        updateDTO.setId(id);
        return Result.ok(recordService.update(updateDTO, getCurrentUserId()), "Record updated");
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('inventory:list')")
    @Operation(summary = "Get record by ID")
    public Result<InventoryRecordVO> getById(@PathVariable Long id) {
        return Result.ok(recordService.getById(id));
    }

    @GetMapping
    @PreAuthorize("@ss.hasAuthority('inventory:list')")
    @Operation(summary = "List records", description = "Paginated records with optional filters")
    public Result<PageVO<InventoryRecordVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Equipment ID filter") @RequestParam(required = false) Long equipmentId,
            @Parameter(description = "Record type: ROUTINE, MAINTENANCE, CALIBRATION, INSPECTION")
            @RequestParam(required = false) String recordType,
            @Parameter(description = "Start date (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (yyyy-MM-dd)") @RequestParam(required = false) String endDate) {
        return Result.ok(recordService.getPage(pageParam.getPageNum(), pageParam.getPageSize(),
                equipmentId, recordType, startDate, endDate));
    }

    @GetMapping("/equipment/{equipmentId}")
    @PreAuthorize("@ss.hasAuthority('inventory:list')")
    @Operation(summary = "Equipment maintenance history", description = "Get maintenance history for a specific equipment")
    public Result<PageVO<InventoryRecordVO>> getByEquipment(
            @PathVariable Long equipmentId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(recordService.getByEquipment(equipmentId, pageNum, pageSize));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('inventory:del')")
    @Operation(summary = "Delete record")
    public Result<Void> delete(
            @PathVariable Long id) {
        recordService.delete(id, getCurrentUserId());
        return Result.ok(null, "Record deleted");
    }

    private Long getCurrentUserId() {
        return securityUtils.currentUserId();
    }
}
