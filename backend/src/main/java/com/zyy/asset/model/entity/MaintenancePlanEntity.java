package com.zyy.asset.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maintenance plan entity for equipment lifecycle management.
 * <p>
 * Maps to the sys_maintenance_plan table. Tracks preventive,
 * corrective, and emergency maintenance plans for equipment assets.
 * <p>
 * Plan types:
 * <ul>
 *   <li>PREVENTIVE  - Scheduled preventive maintenance</li>
 *   <li>CORRECTIVE  - Corrective maintenance</li>
 *   <li>EMERGENCY   - Emergency maintenance</li>
 * </ul>
 * <p>
 * Status flow:
 * <ul>
 *   <li>PENDING      - Plan created, awaiting activation</li>
 *   <li>IN_PROGRESS  - Plan actively being executed</li>
 *   <li>COMPLETED    - Plan finished successfully</li>
 *   <li>CANCELLED    - Plan cancelled</li>
 * </ul>
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_maintenance_plan")
public class MaintenancePlanEntity extends BaseEntity {

    /** Associated equipment ID */
    private Long equipmentId;

    /** Plan display name */
    private String planName;

    /** Plan type: PREVENTIVE, CORRECTIVE, EMERGENCY */
    private String planType;

    /** Plan description */
    private String description;

    /** Scheduled execution date */
    private LocalDate scheduledDate;

    /** Actual completion date */
    private LocalDate completedDate;

    /** Assigned technician or team user ID */
    private Long assignedTo;

    /** Plan status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED */
    private String status;

    /** Priority level: LOW, MEDIUM, HIGH, CRITICAL */
    private String priority;

    /** Estimated or actual cost */
    private BigDecimal cost;

    /** Additional remarks */
    private String remarks;
}
