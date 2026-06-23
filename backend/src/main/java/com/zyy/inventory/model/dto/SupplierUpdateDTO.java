package com.zyy.inventory.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Data Transfer Object for supplier update operations.
 *
 * @author System Architect
 */
@Data
public class SupplierUpdateDTO {

    @NotNull(message = "Supplier ID is required")
    private Long id;

    @Size(max = 128, message = "Supplier name cannot exceed 128 characters")
    private String supplierName;

    @Size(max = 64, message = "Contact person cannot exceed 64 characters")
    private String contactPerson;

    @Size(max = 32, message = "Contact phone cannot exceed 32 characters")
    private String contactPhone;

    @Email(message = "Invalid email format")
    @Size(max = 128, message = "Email cannot exceed 128 characters")
    private String email;

    @Size(max = 256, message = "Address cannot exceed 256 characters")
    private String address;

    private String description;

    private Integer status;
}
