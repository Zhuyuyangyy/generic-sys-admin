package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data Transfer Object for equipment creation.
 *
 * @author System Architect
 */
@Data
public class EquipmentSaveDTO {

    @NotBlank(message = "Equipment code is required")
    @Size(max = 64, message = "Equipment code cannot exceed 64 characters")
    private String equipmentCode;

    @NotBlank(message = "Equipment name is required")
    @Size(max = 128, message = "Equipment name cannot exceed 128 characters")
    private String name;

    @Size(max = 64, message = "Category cannot exceed 64 characters")
    private String category;

    @Size(max = 128, message = "Model cannot exceed 128 characters")
    private String model;

    @Size(max = 128, message = "Manufacturer cannot exceed 128 characters")
    private String manufacturer;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate warrantyExpiry;

    @Min(value = 0, message = "Status must be 0, 1, or 2")
    @Max(value = 2, message = "Status must be 0, 1, or 2")
    private Integer status = 1;

    @Size(max = 256, message = "Location cannot exceed 256 characters")
    private String location;

    @Min(value = 1, message = "Maintenance cycle must be positive")
    private Integer maintenanceCycleDays;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextMaintenanceDate;

    private String remarks;
}
