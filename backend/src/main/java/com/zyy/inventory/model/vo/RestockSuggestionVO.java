package com.zyy.inventory.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * View Object representing a restock suggestion for a consumable.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestockSuggestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Consumable ID */
    private Long consumableId;

    /** Consumable product code */
    private String productCode;

    /** Consumable name */
    private String consumableName;

    /** Current stock quantity */
    private Integer currentStock;

    /** Minimum stock level (safety stock) */
    private Integer minStockLevel;

    /** Average daily consumption (7-day weighted with 30-day) */
    private Double avgDailyConsumption;

    /** 7-day daily consumption */
    private Double consumption7d;

    /** 30-day daily consumption */
    private Double consumption30d;

    /** Days until stockout at current consumption rate */
    private Double daysUntilStockout;

    /** Estimated stockout date */
    private LocalDate estimatedStockoutDate;

    /** Suggested restock quantity */
    private Integer suggestedQuantity;

    /** Supplier lead time in days */
    private Integer leadTimeDays;

    /** Risk level: LOW, MEDIUM, HIGH, CRITICAL */
    private String riskLevel;

    /** Human-readable reasoning */
    private List<String> reasoning;
}
