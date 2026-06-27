package com.zyy.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * View Object for platform-wide statistics.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalTenants;
    private Long activeTenants;
    private Long totalUsers;
    private Long totalAssets;
    private Double revenue;
    private Map<String, Long> byPlan;
}
