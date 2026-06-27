package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data Transfer Object for inspection update operations.
 *
 * @author System Architect
 */
@Data
public class InspectionUpdateDTO {

    @NotNull(message = "Inspection ID is required")
    private Long id;

    @Pattern(regexp = "ROUTINE|SPECIAL|FOLLOW_UP", message = "Inspection type must be ROUTINE, SPECIAL, or FOLLOW_UP")
    private String inspectionType;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate inspectionDate;

    private Long inspectorId;

    @Size(max = 64, message = "Inspector name cannot exceed 64 characters")
    private String inspectorName;

    @Pattern(regexp = "PASS|FAIL|CONDITIONAL", message = "Result must be PASS, FAIL, or CONDITIONAL")
    private String result;

    private String findings;

    private String correctiveAction;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextInspectionDate;
}
