package com.zyy.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * View Object for tenant usage statistics.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantUsageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private String tenantName;
    private Long userCount;
    private Integer maxUsers;
    private Double userUsagePercentage;
    private Long assetCount;
    private Integer maxAssets;
    private Double assetUsagePercentage;
    private Long storageUsed;
    private Long storageLimit;
    private Double storageUsagePercentage;
}
