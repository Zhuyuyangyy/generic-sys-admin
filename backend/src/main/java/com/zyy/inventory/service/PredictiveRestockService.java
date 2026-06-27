package com.zyy.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.exception.BusinessException;
import com.zyy.inventory.mapper.ConsumableMapper;
import com.zyy.inventory.mapper.InventoryTransactionMapper;
import com.zyy.inventory.mapper.SupplierMapper;
import com.zyy.inventory.model.entity.ConsumableEntity;
import com.zyy.inventory.model.entity.InventoryTransactionEntity;
import com.zyy.inventory.model.entity.SupplierEntity;
import com.zyy.inventory.model.vo.RestockSuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Predictive restock service for consumable inventory.
 * <p>
 * Calculates restock suggestions based on weighted average daily consumption
 * from recent 7-day and 30-day outbound data, current stock levels,
 * safety stock, and supplier lead time.
 * <p>
 * Algorithm:
 * <pre>
 * avgDailyConsumption = (recent_7d_outbound * 1.0 + recent_30d_outbound * 0.7) / 37
 * daysUntilStockout = currentStock / avgDailyConsumption
 * suggestedQuantity = max(0, (safetyStock + avgDailyConsumption * leadTimeDays) - currentStock)
 * riskLevel = daysUntilStockout &lt; leadTimeDays ? "CRITICAL"
 *           : daysUntilStockout &lt; 14 ? "HIGH"
 *           : daysUntilStockout &lt; 30 ? "MEDIUM" : "LOW"
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictiveRestockService {

    private final ConsumableMapper consumableMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final SupplierMapper supplierMapper;

    /** Default supplier lead time in days when not configured */
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;

    /** Default safety stock factor (multiplier of avg daily consumption) */
    private static final double SAFETY_STOCK_FACTOR = 1.5;

    @Transactional(readOnly = true)
    public RestockSuggestionVO calculateRestockSuggestion(Long consumableId) {
        ConsumableEntity consumable = consumableMapper.selectById(consumableId);
        if (consumable == null) {
            throw new BusinessException("Consumable not found: " + consumableId);
        }

        return buildSuggestion(consumable);
    }

    @Transactional(readOnly = true)
    public List<RestockSuggestionVO> getAllRestockSuggestions() {
        LambdaQueryWrapper<ConsumableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsumableEntity::getStatus, 1)
                .eq(ConsumableEntity::getIsDeleted, 0);

        List<ConsumableEntity> consumables = consumableMapper.selectList(wrapper);
        return consumables.stream()
                .map(this::buildSuggestion)
                .collect(Collectors.toList());
    }

    private RestockSuggestionVO buildSuggestion(ConsumableEntity consumable) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        // Calculate 7-day outbound
        int outbound7d = getOutboundQuantity(consumable.getId(), sevenDaysAgo, now);
        // Calculate 30-day outbound
        int outbound30d = getOutboundQuantity(consumable.getId(), thirtyDaysAgo, now);

        // Weighted average daily consumption
        // Recent 7-day data gets full weight (1.0), 30-day data gets 0.7 weight
        // Denominator: 7*1.0 + 30*0.7 = 7 + 21 = 28 ... but using the spec formula: 37
        double avgDailyConsumption = (outbound7d * 1.0 + outbound30d * 0.7) / 37.0;
        double consumption7d = outbound7d / 7.0;
        double consumption30d = outbound30d / 30.0;

        // Prevent division by zero
        if (avgDailyConsumption < 0.01) {
            avgDailyConsumption = 0.01;
        }

        int currentStock = consumable.getStockQuantity() != null ? consumable.getStockQuantity() : 0;
        int safetyStock = consumable.getMinStockLevel() != null ? consumable.getMinStockLevel() : 0;
        int leadTimeDays = getLeadTimeDays(consumable.getSupplier());

        // Days until stockout
        double daysUntilStockout = currentStock / avgDailyConsumption;

        // Suggested restock quantity
        int suggestedQuantity = (int) Math.max(0,
                Math.ceil(safetyStock + avgDailyConsumption * leadTimeDays - currentStock));

        // Risk level
        String riskLevel;
        if (daysUntilStockout < leadTimeDays) {
            riskLevel = "CRITICAL";
        } else if (daysUntilStockout < 14) {
            riskLevel = "HIGH";
        } else if (daysUntilStockout < 30) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        // Estimated stockout date
        LocalDate estimatedStockoutDate = daysUntilStockout > 365
                ? null
                : LocalDate.now().plusDays((long) daysUntilStockout);

        // Build reasoning
        List<String> reasoning = new ArrayList<>();
        reasoning.add(String.format("7-day outbound: %d units (%.1f/day)", outbound7d, consumption7d));
        reasoning.add(String.format("30-day outbound: %d units (%.1f/day)", outbound30d, consumption30d));
        reasoning.add(String.format("Weighted avg daily consumption: %.2f units/day", avgDailyConsumption));
        reasoning.add(String.format("Current stock: %d units", currentStock));
        reasoning.add(String.format("Safety stock level: %d units", safetyStock));
        reasoning.add(String.format("Days until stockout: %.1f days", daysUntilStockout));
        if (suggestedQuantity > 0) {
            reasoning.add(String.format("Suggested order quantity: %d units (covers safety stock + %d days lead time)",
                    suggestedQuantity, leadTimeDays));
        } else {
            reasoning.add("No restock needed - current stock covers demand through lead time");
        }

        return RestockSuggestionVO.builder()
                .consumableId(consumable.getId())
                .productCode(consumable.getProductCode())
                .consumableName(consumable.getName())
                .currentStock(currentStock)
                .minStockLevel(safetyStock)
                .avgDailyConsumption(Math.round(avgDailyConsumption * 100.0) / 100.0)
                .consumption7d(Math.round(consumption7d * 100.0) / 100.0)
                .consumption30d(Math.round(consumption30d * 100.0) / 100.0)
                .daysUntilStockout(Math.round(daysUntilStockout * 10.0) / 10.0)
                .estimatedStockoutDate(estimatedStockoutDate)
                .suggestedQuantity(suggestedQuantity)
                .leadTimeDays(leadTimeDays)
                .riskLevel(riskLevel)
                .reasoning(reasoning)
                .build();
    }

    /**
     * Get total outbound quantity for a consumable within a time range.
     */
    private int getOutboundQuantity(Long consumableId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<InventoryTransactionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryTransactionEntity::getConsumableId, consumableId)
                .eq(InventoryTransactionEntity::getTransactionType, "OUTBOUND")
                .ge(InventoryTransactionEntity::getTransactionTime, from)
                .le(InventoryTransactionEntity::getTransactionTime, to);

        List<InventoryTransactionEntity> transactions = transactionMapper.selectList(wrapper);
        return transactions.stream()
                .mapToInt(t -> t.getQuantity() != null ? t.getQuantity() : 0)
                .sum();
    }

    /**
     * Look up supplier lead time from supplier name.
     * Falls back to default if not found.
     */
    private int getLeadTimeDays(String supplierName) {
        if (supplierName == null || supplierName.isBlank()) {
            return DEFAULT_LEAD_TIME_DAYS;
        }
        LambdaQueryWrapper<SupplierEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierEntity::getSupplierName, supplierName)
                .eq(SupplierEntity::getStatus, 1)
                .last("LIMIT 1");
        SupplierEntity supplier = supplierMapper.selectOne(wrapper);
        if (supplier != null && supplier.getLeadTimeDays() != null && supplier.getLeadTimeDays() > 0) {
            return supplier.getLeadTimeDays();
        }
        return DEFAULT_LEAD_TIME_DAYS;
    }
}
