package com.zyy.asset.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View Object for maintenance plan data presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaintenancePlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String planName;
    private String planType;
    private String description;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private Long assignedTo;
    private String status;
    private String priority;
    private BigDecimal cost;
    private String remarks;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
