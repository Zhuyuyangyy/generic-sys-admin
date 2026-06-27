package com.zyy.inventory.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View Object for consumable batch data presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long consumableId;
    private String batchNo;
    private Long supplierId;
    private Integer quantity;
    private Integer remainingQuantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private LocalDate productionDate;
    private LocalDate expirationDate;
    private String status;
    private String storageLocation;
    private LocalDateTime createTime;
}
