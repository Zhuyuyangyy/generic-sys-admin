package com.zyy.asset.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View Object for inspection data presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InspectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long equipmentId;
    private String inspectionType;
    private LocalDate inspectionDate;
    private Long inspectorId;
    private String inspectorName;
    private String result;
    private String findings;
    private String correctiveAction;
    private LocalDate nextInspectionDate;
    private LocalDateTime createTime;
}
