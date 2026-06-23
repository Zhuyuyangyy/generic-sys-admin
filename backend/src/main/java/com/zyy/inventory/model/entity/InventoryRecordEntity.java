package com.zyy.inventory.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Equipment maintenance and inspection record entity.
 * <p>
 * Maps to the sys_inventory_record table. Tracks equipment
 * inspection, maintenance, and calibration history.
 * <p>
 * Record types:
 * <ul>
 *   <li>ROUTINE      - Scheduled routine inspection</li>
 *   <li>MAINTENANCE  - Corrective maintenance</li>
 *   <li>CALIBRATION  - Accuracy calibration</li>
 *   <li>INSPECTION   - Regulatory/compliance inspection</li>
 * </ul>
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_inventory_record")
public class InventoryRecordEntity extends BaseEntity {

    /** Associated equipment ID */
    private Long equipmentId;

    /** Record type classification */
    private String recordType;

    /** Scheduled or actual inspection date */
    private LocalDate recordDate;

    /** Inspector user ID */
    private Long inspectorId;

    /** Inspector display name */
    private String inspectorName;

    /** Maintenance or inspection result */
    private String result;

    /**
     * Overall status of the record.
     * 0 = pending, 1 = completed, 2 = anomaly found
     */
    @TableField("status")
    private Integer status;

    /** Equipment condition at time of record */
    private String equipmentCondition;

    /** Work description or findings */
    private String description;

    /** Materials or parts used */
    private String materialsUsed;

    /** Total man-hours spent */
    private Double manHours;

    /** Next scheduled inspection date */
    private LocalDate nextDate;

    /** Inspector signature or approval reference */
    private String signature;
}
