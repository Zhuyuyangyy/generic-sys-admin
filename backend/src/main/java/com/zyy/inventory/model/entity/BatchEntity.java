package com.zyy.inventory.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Consumable batch entity for tracking batch-level inventory.
 * <p>
 * Maps to the consumable_batch table. Records individual batches
 * of consumable supplies with supplier, cost, and expiration tracking.
 * <p>
 * Batch status:
 * <ul>
 *   <li>ACTIVE    - Batch is active and in stock</li>
 *   <li>EXPIRED   - Batch has passed expiration date</li>
 *   <li>DEPLETED  - Batch stock has been fully consumed</li>
 * </ul>
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("consumable_batch")
public class BatchEntity extends BaseEntity {

    /** Associated consumable ID */
    private Long consumableId;

    /** Batch number for tracking */
    private String batchNo;

    /** Supplier ID */
    private Long supplierId;

    /** Total quantity received in this batch */
    private Integer quantity;

    /** Remaining quantity in this batch */
    private Integer remainingQuantity;

    /** Unit cost for this batch */
    private BigDecimal unitCost;

    /** Total cost for this batch (quantity * unitCost) */
    private BigDecimal totalCost;

    /** Production date */
    private LocalDate productionDate;

    /** Expiration date */
    private LocalDate expirationDate;

    /** Batch status: ACTIVE, EXPIRED, DEPLETED */
    private String status;

    /** Physical storage location */
    private String storageLocation;
}
