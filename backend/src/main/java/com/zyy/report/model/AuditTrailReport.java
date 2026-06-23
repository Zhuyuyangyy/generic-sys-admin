package com.zyy.report.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Audit trail report model.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditTrailReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total number of operations */
    private Integer totalOperations;

    /** Operation count by module */
    private Map<String, Integer> byModule;

    /** Operation count by operation type */
    private Map<String, Integer> byOperation;

    /** Top operators by operation count */
    private List<String> topOperators;

    /** Number of anomalies detected */
    private Integer anomalyCount;
}
