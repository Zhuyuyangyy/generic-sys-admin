package com.zyy.asset.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Asset location entity for hierarchical location management.
 * <p>
 * Maps to the sys_location table. Supports tree-structured
 * location hierarchy via parentId self-reference.
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_location")
public class LocationEntity extends BaseEntity {

    /** Location display name */
    private String locationName;

    /** Unique location code */
    private String locationCode;

    /** Parent location ID (null for root nodes) */
    private Long parentId;

    /** Physical address */
    private String address;

    /** Location description */
    private String description;

    /** Location status: 0=inactive, 1=active */
    private Integer status;
}
