package com.zyy.inventory.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data Transfer Object for maintenance/inspection record update.
 *
 * @author System Architect
 */
@Data
public class InventoryRecordUpdateDTO {

    @NotNull(message = "Record ID is required")
    private Long id;

    @Size(max = 32, message = "Record type cannot exceed 32 characters")
    private String recordType;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @Size(max = 128, message = "Inspector name cannot exceed 128 characters")
    private String inspectorName;

    @Size(max = 32, message = "Result cannot exceed 32 characters")
    private String result;

    @Min(value = 0, message = "Status must be 0, 1, or 2")
    @Max(value = 2, message = "Status must be 0, 1, or 2")
    private Integer status;

    @Size(max = 512, message = "Equipment condition cannot exceed 512 characters")
    private String equipmentCondition;

    @Size(max = 2048, message = "Description cannot exceed 2048 characters")
    private String description;

    @Size(max = 1024, message = "Materials used cannot exceed 1024 characters")
    private String materialsUsed;

    @DecimalMin(value = "0.0", message = "Man hours cannot be negative")
    private Double manHours;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDate;

    @Size(max = 256, message = "Signature cannot exceed 256 characters")
    private String signature;
}
