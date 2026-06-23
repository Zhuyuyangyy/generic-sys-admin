package com.zyy.audit.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * View Object representing a detected anomaly event.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnomalyEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Unique event identifier */
    private String id;

    /** Anomaly type classification */
    private String anomalyType;

    /** Severity level: LOW, MEDIUM, HIGH, CRITICAL */
    private String severity;

    /** Human-readable description of the anomaly */
    private String description;

    /** User ID associated with the anomaly */
    private Long userId;

    /** Username associated with the anomaly */
    private String username;

    /** Timestamp when the anomaly was detected */
    private LocalDateTime detectedAt;

    /** IDs of operation logs related to this anomaly */
    private List<Long> relatedLogIds;

    /** Recommended action to address the anomaly */
    private String recommendation;
}
