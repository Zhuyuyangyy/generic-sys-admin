package com.zyy.inventory.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View Object for equipment maintenance/inspection record presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCode;
    private String recordType;
    private LocalDate recordDate;
    private Long inspectorId;
    private String inspectorName;
    private String result;
    private Integer status;
    private String statusText;
    private String equipmentCondition;
    private String description;
    private String materialsUsed;
    private Double manHours;
    private LocalDate nextDate;
    private String signature;
    private LocalDateTime createTime;
}
