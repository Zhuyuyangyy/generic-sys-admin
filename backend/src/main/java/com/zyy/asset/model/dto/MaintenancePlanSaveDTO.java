package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for maintenance plan creation.
 *
 * @author System Architect
 */
@Data
public class MaintenancePlanSaveDTO {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotBlank(message = "Plan name is required")
    @Size(max = 128, message = "Plan name cannot exceed 128 characters")
    private String planName;

    @NotBlank(message = "Plan type is required")
    @Pattern(regexp = "PREVENTIVE|CORRECTIVE|EMERGENCY", message = "Plan type must be PREVENTIVE, CORRECTIVE, or EMERGENCY")
    private String planType;

    private String description;

    @NotNull(message = "Scheduled date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;

    private Long assignedTo;

    @Pattern(regexp = "PENDING|IN_PROGRESS|COMPLETED|CANCELLED", message = "Status must be PENDING, IN_PROGRESS, COMPLETED, or CANCELLED")
    private String status = "PENDING";

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;

    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    private BigDecimal cost;

    private String remarks;
}
