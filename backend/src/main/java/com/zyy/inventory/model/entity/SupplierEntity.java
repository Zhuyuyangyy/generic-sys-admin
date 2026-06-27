package com.zyy.inventory.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Supplier entity for consumable vendor management.
 * <p>
 * Maps to the sys_supplier table. Tracks supplier information
 * for procurement and inventory replenishment workflows.
 *
 * @author System Architect
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_supplier")
public class SupplierEntity extends BaseEntity {

    /** Unique supplier code */
    private String supplierCode;

    /** Supplier display name */
    private String supplierName;

    /** Primary contact person */
    private String contactPerson;

    /** Contact phone number */
    private String contactPhone;

    /** Contact email address */
    private String email;

    /** Business address */
    private String address;

    /** Supplier description */
    private String description;

    /** Supplier lead time in days */
    private Integer leadTimeDays;

    /** Supplier status: 0=inactive, 1=active */
    private Integer status;
}
