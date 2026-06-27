package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data Transfer Object for equipment assignment creation.
 *
 * @author System Architect
 */
@Data
public class AssignmentSaveDTO {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotNull(message = "Assigned-to user ID is required")
    private Long assignedToUserId;

    @Size(max = 64, message = "Assigned-to user name cannot exceed 64 characters")
    private String assignedToUserName;

    @NotNull(message = "Assigned-by user ID is required")
    private Long assignedByUserId;

    @NotNull(message = "Assigned date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate assignedDate;

    @NotBlank(message = "Assignment type is required")
    @Pattern(regexp = "BORROW|PERMANENT", message = "Assignment type must be BORROW or PERMANENT")
    private String assignmentType;

    private String remarks;
}
