package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for maintenance plan update operations.
 *
 * @author System Architect
 */
@Data
public class MaintenancePlanUpdateDTO {

    @NotNull(message = "Plan ID is required")
    private Long id;

    @Size(max = 128, message = "Plan name cannot exceed 128 characters")
    private String planName;

    @Pattern(regexp = "PREVENTIVE|CORRECTIVE|EMERGENCY", message = "Plan type must be PREVENTIVE, CORRECTIVE, or EMERGENCY")
    private String planType;

    private String description;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate completedDate;

    private Long assignedTo;

    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;

    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    private BigDecimal cost;

    private String remarks;
}
