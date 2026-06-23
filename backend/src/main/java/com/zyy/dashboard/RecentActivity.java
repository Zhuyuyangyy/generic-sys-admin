package com.zyy.dashboard;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Recent activity DTO for the dashboard.
 *
 * @author System Architect
 */
@Data
@Builder
public class RecentActivity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String activityType;
    private String description;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime activityTime;
}
