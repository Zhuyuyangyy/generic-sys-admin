package com.zyy.inventory.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.inventory.model.dto.InventoryRecordSaveDTO;
import com.zyy.inventory.model.dto.InventoryRecordUpdateDTO;
import com.zyy.inventory.model.vo.InventoryRecordVO;
import com.zyy.inventory.service.InventoryRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping
    @Operation(summary = "Create record", description = "Create a new equipment maintenance or inspection record")
    public Result<InventoryRecordVO> save(
            @Valid @RequestBody InventoryRecordSaveDTO saveDTO,
            HttpServletRequest request) {
        InventoryRecordVO vo = recordService.save(saveDTO, getCurrentUserId(request));
        return Result.ok(vo, "Record created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update record")
    public Result<InventoryRecordVO> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRecordUpdateDTO updateDTO,
            HttpServletRequest request) {
        updateDTO.setId(id);
        return Result.ok(recordService.update(updateDTO, getCurrentUserId(request)), "Record updated");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get record by ID")
    public Result<InventoryRecordVO> getById(@PathVariable Long id) {
        return Result.ok(recordService.getById(id));
    }

    @GetMapping
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
    @Operation(summary = "Equipment maintenance history", description = "Get maintenance history for a specific equipment")
    public Result<PageVO<InventoryRecordVO>> getByEquipment(
            @PathVariable Long equipmentId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(recordService.getByEquipment(equipmentId, pageNum, pageSize));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete record")
    public Result<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request) {
        recordService.delete(id, getCurrentUserId(request));
        return Result.ok(null, "Record deleted");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
