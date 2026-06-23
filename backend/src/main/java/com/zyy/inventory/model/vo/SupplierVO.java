package com.zyy.inventory.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View Object for supplier data presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String supplierCode;
    private String supplierName;
    private String contactPerson;
    private String contactPhone;
    private String email;
    private String address;
    private String description;
    private Integer status;
    private String statusText;
    private LocalDateTime createTime;
}
