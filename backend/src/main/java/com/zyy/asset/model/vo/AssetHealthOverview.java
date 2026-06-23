package com.zyy.asset.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * View Object representing an overview of all equipment health.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetHealthOverview implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total number of equipment */
    private Integer totalEquipment;

    /** Average health score across all equipment */
    private Double averageScore;

    /** Distribution of equipment by health grade */
    private Map<String, Integer> gradeDistribution;

    /** Number of equipment at risk (grade C or D) */
    private Integer atRiskCount;

    /** Number of equipment in critical condition (grade D) */
    private Integer criticalCount;
}
