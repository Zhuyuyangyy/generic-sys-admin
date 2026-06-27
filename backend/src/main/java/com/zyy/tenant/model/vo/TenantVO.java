package com.zyy.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View Object for tenant data presentation.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantCode;
    private String tenantName;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String domain;
    private String logoUrl;
    private String subscriptionPlan;
    private Integer maxUsers;
    private Integer maxAssets;
    private Integer status;
    private String statusText;
    private LocalDateTime expiryDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
