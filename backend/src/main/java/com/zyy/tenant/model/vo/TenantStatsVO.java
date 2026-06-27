package com.zyy.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * View Object for tenant statistics.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private String tenantName;
    private Long userCount;
    private Long assetCount;
    private Long storageUsed;
    private String subscriptionPlan;
    private Integer maxUsers;
    private Integer maxAssets;
}
