package com.zyy.dashboard;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Inventory summary DTO for the dashboard.
 *
 * @author System Architect
 */
@Data
@Builder
public class InventorySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalConsumables;
    private Long availableConsumables;
    private Long lowStockCount;
    private Long expiringCount;
    private Long overstockCount;
    private BigDecimal totalInventoryValue;
}
