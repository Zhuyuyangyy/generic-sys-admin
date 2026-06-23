package com.zyy.inventory.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Consumable inventory entity.
 * <p>
 * Maps to the sys_consumable table. Tracks consumable supplies
 * with stock level monitoring and reorder point alerts.
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_consumable")
public class ConsumableEntity extends BaseEntity {

    /** Unique product code */
    private String productCode;

    /** Product display name */
    private String name;

    /** Product category */
    private String category;

    /** Unit of measure */
    private String unit;

    /** Current stock quantity */
    private Integer stockQuantity;

    /** Minimum stock threshold for low-stock alert */
    private Integer minStockLevel;

    /** Maximum stock capacity */
    private Integer maxStockLevel;

    /** Unit cost */
    private BigDecimal unitCost;

    /** Expiration date (nullable for non-perishables) */
    private LocalDate expirationDate;

    /** Supplier name */
    private String supplier;

    /** Storage location in warehouse */
    private String storageLocation;

    /**
     * Availability status.
     * 0 = unavailable, 1 = available
     */
    @TableField("status")
    private Integer status;

    /** Reorder trigger point */
    private Integer reorderPoint;

    /** Last physical inventory check date */
    private LocalDate lastCheckDate;

    /** Additional notes */
    private String remarks;
}
