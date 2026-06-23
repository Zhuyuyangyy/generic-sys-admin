package com.zyy.inventory.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.inventory.model.vo.StockAlertVO;
import com.zyy.inventory.service.StockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RESTful controller for stock alert management.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET   /api/consumables/alerts              - Paginated alert list with type filter</li>
 *   <li>GET   /api/consumables/alerts/summary      - Alert summary counts</li>
 *   <li>PATCH /api/consumables/alerts/{id}/acknowledge - Acknowledge alert</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/consumables/alerts")
@RequiredArgsConstructor
@Tag(name = "Stock Alert Management", description = "Consumable inventory alert monitoring and acknowledgment")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @GetMapping
    @Operation(summary = "List alerts", description = "Paginated active alerts with optional type filter")
    public Result<PageVO<StockAlertVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Alert type filter: LOW_STOCK, EXPIRING, OVERSTOCK") @RequestParam(required = false) String alertType) {
        PageVO<StockAlertVO> page = stockAlertService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), alertType);
        return Result.ok(page);
    }

    @GetMapping("/summary")
    @Operation(summary = "Alert summary", description = "Get unacknowledged alert counts grouped by type")
    public Result<Map<String, Long>> getSummary() {
        Map<String, Long> summary = stockAlertService.getSummary();
        return Result.ok(summary);
    }

    @PatchMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge alert", description = "Mark an alert as acknowledged")
    public Result<Void> acknowledge(
            @Parameter(description = "Alert ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        stockAlertService.acknowledge(id, operatorId);
        return Result.ok(null, "Alert acknowledged");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
