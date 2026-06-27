package com.zyy.iam.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zyy.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Department entity for organizational structure and data scope filtering.
 * Maps to the sys_department table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_department")
public class DepartmentEntity extends BaseEntity {

    /** Department name */
    private String deptName;

    /** Parent department ID, 0 for root level */
    private Long parentId;

    /** Department leader user ID */
    private Long leaderId;

    /** Sort order for display */
    private Integer sort;

    /** Status: 1=active, 0=disabled */
    private Integer status;
}
