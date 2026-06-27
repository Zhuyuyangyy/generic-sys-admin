package com.zyy.asset.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View Object for equipment assignment data presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long equipmentId;
    private Long assignedToUserId;
    private String assignedToUserName;
    private Long assignedByUserId;
    private LocalDate assignedDate;
    private LocalDate returnedDate;
    private String assignmentType;
    private String status;
    private String remarks;
    private LocalDateTime createTime;
}
