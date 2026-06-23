package com.zyy.audit.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View Object for operation log data presentation.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String module;

    private String operation;

    private String method;

    private String requestParams;

    private String responseResult;

    private Long operatorId;

    private String operatorName;

    private String ip;

    private Long duration;

    private Integer status;

    private String statusText;

    private String errorMessage;

    private LocalDateTime createTime;
}
