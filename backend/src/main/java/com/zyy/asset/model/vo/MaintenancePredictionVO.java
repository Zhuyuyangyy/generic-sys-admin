package com.zyy.asset.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * View Object representing a maintenance prediction for equipment.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaintenancePredictionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Equipment ID */
    private Long equipmentId;

    /** Equipment asset code */
    private String equipmentCode;

    /** Equipment display name */
    private String equipmentName;

    /** Equipment category */
    private String category;

    /** Current equipment status */
    private Integer status;

    /** Last maintenance date */
    private LocalDate lastMaintenanceDate;

    /** Next scheduled maintenance date */
    private LocalDate nextMaintenanceDate;

    /** Predicted next maintenance date based on cycle and history */
    private LocalDate predictedNextMaintenanceDate;

    /** Maintenance cycle in days */
    private Integer maintenanceCycleDays;

    /** Days since last maintenance */
    private Long daysSinceLastMaintenance;

    /** Days until next scheduled maintenance */
    private Long daysUntilNextMaintenance;

    /** Equipment age in days since purchase */
    private Long equipmentAgeDays;

    /** Failure rate: number of emergency/corrective maintenances in last 180 days */
    private Integer recentFailureCount;

    /** Maintenance risk: risk of missing next scheduled maintenance */
    private String maintenanceRisk;

    /** Overall risk level: LOW, MEDIUM, HIGH, CRITICAL */
    private String riskLevel;

    /** Human-readable reasoning */
    private List<String> reasoning;
}
