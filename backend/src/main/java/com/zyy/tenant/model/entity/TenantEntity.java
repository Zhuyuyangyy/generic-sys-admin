package com.zyy.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zyy.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Tenant entity for multi-tenant management.
 * Maps to the sys_tenant table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class TenantEntity extends BaseEntity {

    /** Unique tenant code */
    private String tenantCode;

    /** Tenant display name */
    private String tenantName;

    /** Contact person name */
    private String contactPerson;

    /** Contact email address */
    private String contactEmail;

    /** Contact phone number */
    private String contactPhone;

    /** Custom domain for tenant access */
    private String domain;

    /** Tenant logo URL */
    private String logoUrl;

    /**
     * Subscription plan type.
     * FREE / BASIC / PROFESSIONAL / ENTERPRISE
     */
    private String subscriptionPlan;

    /** Maximum allowed users for this tenant */
    private Integer maxUsers;

    /** Maximum allowed assets for this tenant */
    private Integer maxAssets;

    /**
     * Tenant status.
     * 1 = ACTIVE, 0 = SUSPENDED, 2 = TERMINATED
     */
    private Integer status;

    /** Subscription expiry date */
    private LocalDateTime expiryDate;
}
