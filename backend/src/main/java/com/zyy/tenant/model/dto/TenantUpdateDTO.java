package com.zyy.tenant.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating an existing tenant.
 */
@Data
public class TenantUpdateDTO {

    private Long id;

    @Size(max = 128, message = "Tenant name cannot exceed 128 characters")
    private String tenantName;

    @Size(max = 64, message = "Contact person name cannot exceed 64 characters")
    private String contactPerson;

    @Email(message = "Invalid email format")
    @Size(max = 128, message = "Contact email cannot exceed 128 characters")
    private String contactEmail;

    @Size(max = 32, message = "Contact phone cannot exceed 32 characters")
    private String contactPhone;

    @Size(max = 256, message = "Domain cannot exceed 256 characters")
    private String domain;

    @Size(max = 512, message = "Logo URL cannot exceed 512 characters")
    private String logoUrl;

    /** Subscription plan: FREE, BASIC, PROFESSIONAL, ENTERPRISE */
    private String subscriptionPlan;

    private Integer maxUsers;

    private Integer maxAssets;
}
