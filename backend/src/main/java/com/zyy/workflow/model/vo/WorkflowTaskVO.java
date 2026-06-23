package com.zyy.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long instanceId;
    private Integer stepOrder;
    private Long assigneeId;
    private String action;
    private String comment;
    private String status;
    private LocalDateTime actionTime;
    private LocalDateTime createTime;
}
