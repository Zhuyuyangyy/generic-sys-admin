package com.zyy.asset.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Equipment entity for inventory asset tracking.
 * <p>
 * Maps to the sys_equipment table. Represents durable equipment
 * assets with maintenance scheduling and warranty tracking.
 * <p>
 * Status state machine:
 * <ul>
 *   <li>0 = Under Maintenance</li>
 *   <li>1 = Normal / Operational</li>
 *   <li>2 = Scrapped / Decommissioned</li>
 * </ul>
 *
 * @author System Architect
 * @see com.zyy.asset.model.vo.EquipmentVO
 * @see com.zyy.asset.model.dto.EquipmentSaveDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_equipment")
public class EquipmentEntity extends BaseEntity {

    /** Unique equipment asset code */
    private String equipmentCode;

    /** Equipment display name */
    private String name;

    /** Equipment category classification */
    private String category;

    /** Equipment model designation */
    private String model;

    /** Manufacturer name */
    private String manufacturer;

    /** Date of purchase */
    private LocalDate purchaseDate;

    /** Warranty expiration date */
    private LocalDate warrantyExpiry;

    /**
     * Operational status.
     * 0 = maintenance, 1 = normal, 2 = scrapped
     */
    @TableField("status")
    private Integer status;

    /** Physical storage or installation location */
    private String location;

    /** Preventive maintenance interval in days */
    private Integer maintenanceCycleDays;

    /** Next scheduled maintenance date */
    private LocalDate nextMaintenanceDate;

    /** Additional notes or remarks */
    private String remarks;
}
