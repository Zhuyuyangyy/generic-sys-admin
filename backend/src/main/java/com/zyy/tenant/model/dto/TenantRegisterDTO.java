package com.zyy.tenant.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for registering a new tenant.
 */
@Data
public class TenantRegisterDTO {

    @NotBlank(message = "Tenant code is required")
    @Size(min = 2, max = 64, message = "Tenant code length must be between 2 and 64 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Tenant code must start with uppercase letter and contain only uppercase alphanumeric characters and underscores")
    private String tenantCode;

    @NotBlank(message = "Tenant name is required")
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

    /** Subscription plan: FREE, BASIC, PROFESSIONAL, ENTERPRISE. Defaults to FREE if not specified. */
    private String subscriptionPlan;

    private Integer maxUsers;

    private Integer maxAssets;
}
