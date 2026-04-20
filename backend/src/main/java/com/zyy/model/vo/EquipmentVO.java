package com.zyy.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View Object for equipment data presentation.
 * <p>
 * Transforms {@link com.zyy.model.entity.EquipmentEntity} for API responses.
 * Includes computed fields (maintenance overdue, warranty expired) derived
 * from the entity state.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EquipmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String equipmentCode;
    private String name;
    private String category;
    private String model;
    private String manufacturer;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private Integer status;
    private String statusText;
    private String location;
    private Integer maintenanceCycleDays;
    private LocalDate nextMaintenanceDate;
    private Boolean maintenanceOverdue;
    private Boolean warrantyExpired;
    private String remarks;
    private LocalDateTime createTime;
}
