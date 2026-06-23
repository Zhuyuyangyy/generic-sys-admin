package com.zyy.asset.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * View Object representing the health score of a single equipment asset.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetHealthScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Equipment ID */
    private Long equipmentId;

    /** Equipment asset code */
    private String equipmentCode;

    /** Equipment display name */
    private String equipmentName;

    /** Health score (0-100) */
    private Integer score;

    /** Health grade: A (90-100), B (70-89), C (50-69), D (0-49) */
    private String grade;

    /** Factors contributing to the health score */
    private List<String> factors;

    /** Last maintenance date */
    private LocalDate lastMaintenanceDate;

    /** Next scheduled maintenance date */
    private LocalDate nextMaintenanceDate;

    /** Whether maintenance is overdue */
    private Boolean isOverdue;

    /** Whether warranty has expired */
    private Boolean warrantyExpired;
}
