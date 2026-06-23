package com.zyy.asset.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Data Transfer Object for location creation.
 *
 * @author System Architect
 */
@Data
public class LocationSaveDTO {

    @NotBlank(message = "Location name is required")
    @Size(max = 128, message = "Location name cannot exceed 128 characters")
    private String locationName;

    @NotBlank(message = "Location code is required")
    @Size(max = 64, message = "Location code cannot exceed 64 characters")
    private String locationCode;

    private Long parentId;

    @Size(max = 256, message = "Address cannot exceed 256 characters")
    private String address;

    private String description;

    private Integer status = 1;
}
