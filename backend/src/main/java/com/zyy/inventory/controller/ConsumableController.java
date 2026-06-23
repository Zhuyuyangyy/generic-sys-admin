package com.zyy.inventory.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.inventory.model.dto.ConsumableSaveDTO;
import com.zyy.inventory.model.dto.ConsumableUpdateDTO;
import com.zyy.inventory.model.vo.ConsumableVO;
import com.zyy.inventory.service.ConsumableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for consumable inventory management.
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/consumables")
@RequiredArgsConstructor
@Tag(name = "Consumable Management", description = "Consumable inventory lifecycle and stock management")
public class ConsumableController {

    private final ConsumableService consumableService;

    @PostMapping
    @Operation(summary = "Register consumable", description = "Add a new consumable product to inventory")
    public Result<ConsumableVO> save(
            @Valid @RequestBody ConsumableSaveDTO saveDTO,
            HttpServletRequest request) {
        ConsumableVO vo = consumableService.save(saveDTO, getCurrentUserId(request));
        return Result.ok(vo, "Consumable registered successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update consumable")
    public Result<ConsumableVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ConsumableUpdateDTO updateDTO,
            HttpServletRequest request) {
        updateDTO.setId(id);
        ConsumableVO vo = consumableService.update(updateDTO, getCurrentUserId(request));
        return Result.ok(vo, "Consumable updated successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consumable by ID")
    public Result<ConsumableVO> getById(
            @PathVariable Long id) {
        return Result.ok(consumableService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List consumables", description = "Paginated list with optional filters")
    public Result<PageVO<ConsumableVO>> getPage(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        return Result.ok(consumableService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), name, category, status));
    }

    @PostMapping("/{id}/inbound")
    @Operation(summary = "Stock inbound", description = "Record incoming stock (procurement, return)")
    public Result<Void> inbound(
            @PathVariable Long id,
            @Parameter(description = "Quantity to add") @RequestParam Integer quantity,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request) {
        consumableService.inbound(id, quantity, referenceNo, remarks, getCurrentUserId(request));
        return Result.ok(null, "Inbound recorded");
    }

    @PostMapping("/{id}/outbound")
    @Operation(summary = "Stock outbound", description = "Record outgoing stock (usage, issuance)")
    public Result<Void> outbound(
            @PathVariable Long id,
            @Parameter(description = "Quantity to deduct") @RequestParam Integer quantity,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request) {
        consumableService.outbound(id, quantity, referenceNo, remarks, getCurrentUserId(request));
        return Result.ok(null, "Outbound recorded");
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Adjust stock", description = "Manual stock adjustment (positive or negative)")
    public Result<Void> adjustStock(
            @PathVariable Long id,
            @Parameter(description = "Quantity change (+/-)") @RequestParam Integer delta,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request) {
        consumableService.adjustStock(id, delta, referenceNo, remarks, getCurrentUserId(request));
        return Result.ok(null, "Stock adjusted");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete consumable")
    public Result<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request) {
        consumableService.delete(id, getCurrentUserId(request));
        return Result.ok(null, "Consumable deleted");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
