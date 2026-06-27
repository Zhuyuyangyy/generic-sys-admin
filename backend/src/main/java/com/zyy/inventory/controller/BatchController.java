package com.zyy.inventory.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.inventory.model.dto.BatchSaveDTO;
import com.zyy.inventory.model.dto.BatchUpdateDTO;
import com.zyy.inventory.model.vo.BatchVO;
import com.zyy.inventory.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for consumable batch management.
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/consumables/batches")
@RequiredArgsConstructor
@Tag(name = "Consumable Batch", description = "Consumable batch and inbound management")
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    @Operation(summary = "Create batch", description = "Record a new consumable batch (inbound)")
    public Result<BatchVO> save(
            @Valid @RequestBody BatchSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        BatchVO vo = batchService.save(saveDTO, operatorId);
        return Result.ok(vo, "Batch created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update batch", description = "Update batch details")
    public Result<BatchVO> update(
            @Parameter(description = "Batch ID") @PathVariable Long id,
            @Valid @RequestBody BatchUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        BatchVO vo = batchService.update(updateDTO, operatorId);
        return Result.ok(vo, "Batch updated successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get batch by ID")
    public Result<BatchVO> getById(
            @Parameter(description = "Batch ID") @PathVariable Long id) {
        BatchVO vo = batchService.getById(id);
        return Result.ok(vo);
    }

    @GetMapping
    @Operation(summary = "List batches", description = "Paginated batch list with optional filters")
    public Result<PageVO<BatchVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Consumable ID filter") @RequestParam(required = false) Long consumableId,
            @Parameter(description = "Status filter: ACTIVE/EXPIRED/DEPLETED") @RequestParam(required = false) String status) {
        PageVO<BatchVO> page = batchService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), consumableId, status);
        return Result.ok(page);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update batch status", description = "Change batch status (ACTIVE/EXPIRED/DEPLETED)")
    public Result<Void> updateStatus(
            @Parameter(description = "Batch ID") @PathVariable Long id,
            @Parameter(description = "Target status: ACTIVE/EXPIRED/DEPLETED") @RequestParam String status,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        batchService.updateStatus(id, status, operatorId);
        return Result.ok(null, "Batch status updated");
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring batches", description = "List batches expiring within specified days")
    public Result<List<BatchVO>> getExpiringBatches(
            @Parameter(description = "Number of days to look ahead") @RequestParam(defaultValue = "30") int days) {
        List<BatchVO> batches = batchService.getExpiringBatches(days);
        return Result.ok(batches);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
