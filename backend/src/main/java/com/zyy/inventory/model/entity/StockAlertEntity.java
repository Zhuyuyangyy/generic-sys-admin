package com.zyy.inventory.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Stock alert entity for consumable inventory monitoring.
 * <p>
 * Maps to the sys_stock_alert table. Tracks low-stock, expiring,
 * and overstock alerts for consumable inventory items.
 * <p>
 * Alert types:
 * <ul>
 *   <li>LOW_STOCK  - Stock below minimum threshold</li>
 *   <li>EXPIRING   - Product approaching expiration date</li>
 *   <li>OVERSTOCK  - Stock exceeds maximum capacity</li>
 * </ul>
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stock_alert")
public class StockAlertEntity extends BaseEntity {

    /** Associated consumable ID */
    private Long consumableId;

    /** Alert type: LOW_STOCK, EXPIRING, OVERSTOCK */
    private String alertType;

    /** Alert message describing the condition */
    private String message;

    /** Threshold value that triggered the alert */
    private Integer threshold;

    /** Current value at the time of alert */
    private Integer currentValue;

    /** Whether the alert has been acknowledged */
    private Boolean acknowledged;

    /** User ID who acknowledged the alert */
    private Long acknowledgedBy;

    /** Timestamp when the alert was acknowledged */
    private LocalDateTime acknowledgedAt;
}
