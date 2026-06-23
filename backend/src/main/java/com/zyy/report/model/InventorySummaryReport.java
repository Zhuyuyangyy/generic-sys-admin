package com.zyy.report.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Consumable inventory summary report model.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventorySummaryReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total number of consumable items */
    private Integer totalConsumables;

    /** Total inventory value */
    private BigDecimal totalValue;

    /** Number of low stock items */
    private Integer lowStockItems;

    /** Number of items approaching expiration */
    private Integer expiringItems;

    /** Consumable count by category */
    private Map<String, Integer> byCategory;

    /** Top suppliers by item count */
    private List<String> topSuppliers;
}
