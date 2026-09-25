package com.zyy.controller;

import com.zyy.common.PageParam;
import com.zyy.common.Result;
import com.zyy.model.dto.ConsumableSaveDTO;
import com.zyy.model.dto.ConsumableUpdateDTO;
import com.zyy.model.vo.ConsumableVO;
import com.zyy.model.vo.PageVO;
import com.zyy.security.SecurityUtils;
import com.zyy.service.impl.ConsumableServiceImpl.ReplayedRequestException;
import com.zyy.service.ConsumableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** 幂等键请求头。缺失时按未保护处理（向后兼容既有客户端）。 */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final ConsumableService consumableService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("@ss.hasAuthority('consumable:add')")
    @Operation(summary = "Register consumable", description = "Add a new consumable product to inventory")
    public Result<ConsumableVO> save(
            @Valid @RequestBody ConsumableSaveDTO saveDTO) {
        ConsumableVO vo = consumableService.save(saveDTO, getCurrentUserId());
        return Result.ok(vo, "Consumable registered successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('consumable:edit')")
    @Operation(summary = "Update consumable")
    public Result<ConsumableVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ConsumableUpdateDTO updateDTO) {
        updateDTO.setId(id);
        ConsumableVO vo = consumableService.update(updateDTO, getCurrentUserId());
        return Result.ok(vo, "Consumable updated successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('consumable:detail')")
    @Operation(summary = "Get consumable by ID")
    public Result<ConsumableVO> getById(
            @PathVariable Long id) {
        return Result.ok(consumableService.getById(id));
    }

    @GetMapping
    @PreAuthorize("@ss.hasAuthority('consumable:list')")
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
    @PreAuthorize("@ss.hasAuthority('consumable:in')")
    @Operation(summary = "Stock inbound", description = "Record incoming stock (procurement, return)")
    public Result<Void> inbound(
            @PathVariable Long id,
            @Parameter(description = "Quantity to add") @RequestParam Integer quantity,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) String remarks,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        try {
            consumableService.inbound(id, quantity, referenceNo, remarks,
                    getCurrentUserId(), idempotencyKey);
        } catch (ReplayedRequestException replay) {
            return Result.ok(null, "Inbound already recorded");
        }
        return Result.ok(null, "Inbound recorded");
    }

    @PostMapping("/{id}/outbound")
    @PreAuthorize("@ss.hasAuthority('consumable:out')")
    @Operation(summary = "Stock outbound", description = "Record outgoing stock (usage, issuance)")
    public Result<Void> outbound(
            @PathVariable Long id,
            @Parameter(description = "Quantity to deduct") @RequestParam Integer quantity,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) String remarks,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        try {
            consumableService.outbound(id, quantity, referenceNo, remarks,
                    getCurrentUserId(), idempotencyKey);
        } catch (ReplayedRequestException replay) {
            return Result.ok(null, "Outbound already recorded");
        }
        return Result.ok(null, "Outbound recorded");
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("@ss.hasAuthority('consumable:edit')")
    @Operation(summary = "Adjust stock", description = "Manual stock adjustment (positive or negative)")
    public Result<Void> adjustStock(
            @PathVariable Long id,
            @Parameter(description = "Quantity change (+/-)") @RequestParam Integer delta,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) String remarks,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        try {
            consumableService.adjustStock(id, delta, referenceNo, remarks,
                    getCurrentUserId(), idempotencyKey);
        } catch (ReplayedRequestException replay) {
            return Result.ok(null, "Stock already adjusted");
        }
        return Result.ok(null, "Stock adjusted");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('consumable:del')")
    @Operation(summary = "Delete consumable")
    public Result<Void> delete(
            @PathVariable Long id) {
        consumableService.delete(id, getCurrentUserId());
        return Result.ok(null, "Consumable deleted");
    }

    private Long getCurrentUserId() {
        return securityUtils.currentUserId();
    }
}
