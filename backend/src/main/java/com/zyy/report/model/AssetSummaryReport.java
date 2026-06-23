package com.zyy.report.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Asset inventory summary report model.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetSummaryReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total number of equipment */
    private Integer totalEquipment;

    /** Equipment count by status */
    private Map<String, Integer> byStatus;

    /** Equipment count by category */
    private Map<String, Integer> byCategory;

    /** Average equipment age in years */
    private Double averageAge;

    /** Number of equipment with overdue maintenance */
    private Integer overdueMaintenance;

    /** Equipment count by health grade distribution */
    private Map<String, Integer> healthDistribution;
}
