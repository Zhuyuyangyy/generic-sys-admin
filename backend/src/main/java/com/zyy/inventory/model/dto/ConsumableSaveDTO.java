package com.zyy.inventory.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for consumable creation.
 *
 * @author System Architect
 */
@Data
public class ConsumableSaveDTO {

    @NotBlank(message = "Product code is required")
    @Size(max = 64, message = "Product code cannot exceed 64 characters")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 128, message = "Product name cannot exceed 128 characters")
    private String name;

    @Size(max = 64, message = "Category cannot exceed 64 characters")
    private String category;

    @NotBlank(message = "Unit is required")
    @Size(max = 32, message = "Unit cannot exceed 32 characters")
    private String unit = "piece";

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity = 0;

    @Min(value = 0, message = "Minimum stock level cannot be negative")
    private Integer minStockLevel = 0;

    private Integer maxStockLevel;

    @DecimalMin(value = "0.00", message = "Unit cost cannot be negative")
    private BigDecimal unitCost = BigDecimal.ZERO;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @Size(max = 128, message = "Supplier cannot exceed 128 characters")
    private String supplier;

    @Size(max = 256, message = "Storage location cannot exceed 256 characters")
    private String storageLocation;

    @Min(value = 0, message = "Status must be 0 or 1")
    @Max(value = 1, message = "Status must be 0 or 1")
    private Integer status = 1;

    private Integer reorderPoint;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastCheckDate;

    private String remarks;
}
