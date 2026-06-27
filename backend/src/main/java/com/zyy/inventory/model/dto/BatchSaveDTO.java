package com.zyy.inventory.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for consumable batch creation (inbound).
 *
 * @author System Architect
 */
@Data
public class BatchSaveDTO {

    @NotNull(message = "Consumable ID is required")
    private Long consumableId;

    @NotBlank(message = "Batch number is required")
    @Size(max = 64, message = "Batch number cannot exceed 64 characters")
    private String batchNo;

    private Long supplierId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0.00", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @Size(max = 256, message = "Storage location cannot exceed 256 characters")
    private String storageLocation;
}
