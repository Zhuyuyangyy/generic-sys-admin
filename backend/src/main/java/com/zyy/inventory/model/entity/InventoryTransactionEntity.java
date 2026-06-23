package com.zyy.inventory.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Inventory transaction ledger entity.
 * <p>
 * Maps to the sys_inventory_transaction table. Provides an immutable
 * audit trail of all consumable stock movements.
 * <p>
 * Transaction types:
 * <ul>
 *   <li>INBOUND    - Stock added (procurement, return)</li>
 *   <li>OUTBOUND   - Stock removed (usage, issuance)</li>
 *   <li>ADJUSTMENT - Manual correction</li>
 *   <li>RETURN     - Item returned to inventory</li>
 * </ul>
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_inventory_transaction")
public class InventoryTransactionEntity extends BaseEntity {

    /** Consumable product ID */
    private Long consumableId;

    /** Transaction type: INBOUND, OUTBOUND, ADJUSTMENT, RETURN */
    private String transactionType;

    /** Transaction quantity (positive or negative) */
    private Integer quantity;

    /** Stock balance after this transaction */
    private Integer balanceAfter;

    /** Reference document number (purchase order, requisition, etc.) */
    private String referenceNo;

    /** Operator user ID */
    private Long operatorId;

    /** Transaction remarks or notes */
    private String remarks;

    /** Transaction timestamp */
    @TableField("transaction_time")
    private LocalDateTime transactionTime;
}
