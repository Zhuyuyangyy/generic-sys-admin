package com.zyy.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity class providing common audit fields for all persistent entities.
 * <p>
 * Implements the Active Record pattern with MyBatis-Plus, providing
 * automatic timestamp management and logical deletion support.
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Primary key identifier */
    @TableId(type = IdType.ASSIGN_ID)
    protected Long id;

    /** Record creation timestamp */
    @TableField(fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    /** Record last modification timestamp */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;

    /** Record creator user ID */
    protected Long createUser;

    /** Tenant identifier for multi-tenancy support */
    @TableField(fill = FieldFill.INSERT)
    protected Long tenantId;

    /** Logical deletion flag: 0=active, 1=deleted */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    protected Integer isDeleted;
}
