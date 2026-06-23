package com.zyy.audit.service;

import com.zyy.audit.model.vo.AnomalyEvent;
import com.zyy.audit.model.vo.AnomalySummary;

import java.util.List;

/**
 * Anomaly detection business service interface.
 */
public interface AnomalyDetectionService {

    /**
     * Scan recent operations for anomalies.
     *
     * @return list of detected anomaly events
     */
    List<AnomalyEvent> detectAnomalies();

    /**
     * Get counts of anomalies grouped by type.
     *
     * @return anomaly summary
     */
    AnomalySummary getAnomalySummary();

    /**
     * Get anomalies detected within the last N hours.
     *
     * @param hours lookback window in hours
     * @return list of anomaly events within the time window
     */
    List<AnomalyEvent> getRecentAnomalies(int hours);
}
