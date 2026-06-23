package com.zyy.audit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Operation log persistent entity.
 * Maps to the operation_log table.
 */
@Data
@TableName("operation_log")
public class OperationLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Module name */
    private String module;

    /** Operation type */
    private String operation;

    /** Method name */
    private String method;

    /** Request parameters (JSON) */
    private String requestParams;

    /** Response result (JSON) */
    private String responseResult;

    /** Operator user ID */
    private Long operatorId;

    /** Operator name */
    private String operatorName;

    /** Client IP address */
    private String ip;

    /** Execution duration in milliseconds */
    private Long duration;

    /** Status: 1=success, 0=failure */
    private Integer status;

    /** Error message (if failed) */
    private String errorMessage;

    /** Log creation timestamp */
    private LocalDateTime createTime;
}
