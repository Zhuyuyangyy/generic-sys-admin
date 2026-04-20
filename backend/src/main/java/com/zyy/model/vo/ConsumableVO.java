package com.zyy.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View Object for consumable inventory data presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumableVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String productCode;
    private String name;
    private String category;
    private String unit;
    private Integer stockQuantity;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private BigDecimal unitCost;
    private LocalDate expirationDate;
    private String supplier;
    private String storageLocation;
    private Integer status;
    private String statusText;
    private Integer reorderPoint;
    private LocalDate lastCheckDate;
    private Boolean lowStockAlert;
    private Boolean expiringAlert;
    private String remarks;
    private LocalDateTime createTime;
}
