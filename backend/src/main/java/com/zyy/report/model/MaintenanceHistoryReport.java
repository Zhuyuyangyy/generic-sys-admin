package com.zyy.report.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Maintenance history report model.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaintenanceHistoryReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total maintenance records */
    private Integer totalRecords;

    /** Maintenance count by type */
    private Map<String, Integer> byType;

    /** Average maintenance cost */
    private BigDecimal averageCost;

    /** Maintenance count by month (format: yyyy-MM) */
    private Map<String, Integer> byMonth;

    /** Top equipment by maintenance frequency */
    private List<String> topEquipment;
}
