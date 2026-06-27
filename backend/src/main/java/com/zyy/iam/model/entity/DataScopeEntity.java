package com.zyy.iam.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zyy.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data scope configuration entity for ABAC data permissions.
 * Maps to the data_scope table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_scope")
public class DataScopeEntity extends BaseEntity {

    /** Role code this data scope applies to */
    private String roleCode;

    /**
     * Scope type defining the data visibility.
     * SELF: Only data created by the user
     * DEPARTMENT: Data from user's department
     * DEPARTMENT_AND_SUB: Data from user's department and sub-departments
     * ALL: All data (no filter)
     * CUSTOM: Custom department list
     */
    private String scopeType;

    /** Custom scope definition (JSON array of department IDs for CUSTOM type) */
    private String customScope;
}
