package com.zyy.asset.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Equipment assignment entity for tracking equipment allocation to users.
 * <p>
 * Maps to the asset_assignment table. Records who equipment is assigned to,
 * whether it is borrowed or permanently assigned, and return tracking.
 * <p>
 * Assignment types:
 * <ul>
 *   <li>BORROW     - Temporary borrowing with expected return</li>
 *   <li>PERMANENT  - Permanent assignment</li>
 * </ul>
 * <p>
 * Status flow:
 * <ul>
 *   <li>ACTIVE   - Currently assigned</li>
 *   <li>RETURNED - Equipment has been returned</li>
 * </ul>
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_assignment")
public class AssignmentEntity extends BaseEntity {

    /** Associated equipment ID */
    private Long equipmentId;

    /** User ID the equipment is assigned to */
    private Long assignedToUserId;

    /** Display name of the assigned user */
    private String assignedToUserName;

    /** User ID who performed the assignment */
    private Long assignedByUserId;

    /** Date of assignment */
    private LocalDate assignedDate;

    /** Date of return (null if not yet returned) */
    private LocalDate returnedDate;

    /** Assignment type: BORROW, PERMANENT */
    private String assignmentType;

    /** Assignment status: ACTIVE, RETURNED */
    private String status;

    /** Additional remarks */
    private String remarks;
}
