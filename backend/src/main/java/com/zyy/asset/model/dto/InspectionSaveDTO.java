package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data Transfer Object for inspection creation.
 *
 * @author System Architect
 */
@Data
public class InspectionSaveDTO {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotBlank(message = "Inspection type is required")
    @Pattern(regexp = "ROUTINE|SPECIAL|FOLLOW_UP", message = "Inspection type must be ROUTINE, SPECIAL, or FOLLOW_UP")
    private String inspectionType;

    @NotNull(message = "Inspection date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate inspectionDate;

    private Long inspectorId;

    @Size(max = 64, message = "Inspector name cannot exceed 64 characters")
    private String inspectorName;

    @NotBlank(message = "Inspection result is required")
    @Pattern(regexp = "PASS|FAIL|CONDITIONAL", message = "Result must be PASS, FAIL, or CONDITIONAL")
    private String result;

    private String findings;

    private String correctiveAction;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextInspectionDate;
}
