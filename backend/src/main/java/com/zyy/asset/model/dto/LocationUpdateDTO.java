package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Data Transfer Object for location update operations.
 *
 * @author System Architect
 */
@Data
public class LocationUpdateDTO {

    @NotNull(message = "Location ID is required")
    private Long id;

    @Size(max = 128, message = "Location name cannot exceed 128 characters")
    private String locationName;

    @Size(max = 64, message = "Location code cannot exceed 64 characters")
    private String locationCode;

    private Long parentId;

    @Size(max = 256, message = "Address cannot exceed 256 characters")
    private String address;

    private String description;

    private Integer status;
}
