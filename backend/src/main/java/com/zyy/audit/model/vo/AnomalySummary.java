package com.zyy.audit.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * View Object representing a summary of detected anomalies.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnomalySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total number of anomalies detected */
    private Integer totalAnomalies;

    /** Count of CRITICAL severity anomalies */
    private Integer criticalCount;

    /** Count of HIGH severity anomalies */
    private Integer highCount;

    /** Count of MEDIUM severity anomalies */
    private Integer mediumCount;

    /** Count of LOW severity anomalies */
    private Integer lowCount;

    /** Anomaly counts grouped by type */
    private Map<String, Integer> byType;
}
