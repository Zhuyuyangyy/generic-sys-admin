package com.zyy.dashboard;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Equipment status distribution DTO for the dashboard.
 *
 * @author System Architect
 */
@Data
@Builder
public class EquipmentStatusDistribution implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long normalCount;
    private Long maintenanceCount;
    private Long scrappedCount;
    private Double normalPercentage;
    private Double maintenancePercentage;
    private Double scrappedPercentage;
}
