package com.zyy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作模块 */
    private String module;

    /** 操作类型 */
    private String operation;

    /** 操作方法名 */
    private String methodName;

    /** 目标表 */
    private String targetTable;

    /** 目标ID */
    private String targetId;

    /** 请求方法 */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParams;

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作人IP */
    private String ipAddress;

    /** User-Agent */
    private String userAgent;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operationTime;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 响应状态：0=失败，1=成功 */
    private Integer resultStatus;

    /** 错误详情 */
    private String errorDetail;
}