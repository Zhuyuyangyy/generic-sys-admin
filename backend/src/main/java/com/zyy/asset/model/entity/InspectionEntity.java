package com.zyy.asset.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Equipment inspection entity for tracking inspection records.
 * <p>
 * Maps to the asset_inspection table. Records equipment inspections
 * including routine, special, and follow-up inspections.
 * <p>
 * Inspection types:
 * <ul>
 *   <li>ROUTINE    - Regular scheduled inspection</li>
 *   <li>SPECIAL    - Special/extraordinary inspection</li>
 *   <li>FOLLOW_UP  - Follow-up inspection after issues found</li>
 * </ul>
 * <p>
 * Results:
 * <ul>
 *   <li>PASS        - Inspection passed</li>
 *   <li>FAIL        - Inspection failed</li>
 *   <li>CONDITIONAL - Conditionally passed with caveats</li>
 * </ul>
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_inspection")
public class InspectionEntity extends BaseEntity {

    /** Associated equipment ID */
    private Long equipmentId;

    /** Inspection type: ROUTINE, SPECIAL, FOLLOW_UP */
    private String inspectionType;

    /** Inspection date */
    private LocalDate inspectionDate;

    /** Inspector user ID */
    private Long inspectorId;

    /** Inspector display name */
    private String inspectorName;

    /** Inspection result: PASS, FAIL, CONDITIONAL */
    private String result;

    /** Inspection findings and observations */
    private String findings;

    /** Corrective action taken or recommended */
    private String correctiveAction;

    /** Next scheduled inspection date */
    private LocalDate nextInspectionDate;
}
