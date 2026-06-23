package com.zyy.inventory.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for consumable update operations.
 *
 * @author System Architect
 */
@Data
public class ConsumableUpdateDTO {

    @NotNull(message = "Consumable ID is required")
    private Long id;

    @Size(max = 128, message = "Product name cannot exceed 128 characters")
    private String name;

    @Size(max = 64, message = "Category cannot exceed 64 characters")
    private String category;

    @Size(max = 32, message = "Unit cannot exceed 32 characters")
    private String unit;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 0, message = "Minimum stock level cannot be negative")
    private Integer minStockLevel;

    private Integer maxStockLevel;

    @DecimalMin(value = "0.00", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @Size(max = 128, message = "Supplier cannot exceed 128 characters")
    private String supplier;

    @Size(max = 256, message = "Storage location cannot exceed 256 characters")
    private String storageLocation;

    @Min(value = 0, message = "Status must be 0 or 1")
    @Max(value = 1, message = "Status must be 0 or 1")
    private Integer status;

    private Integer reorderPoint;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastCheckDate;

    private String remarks;
}
