package com.zyy.inventory.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View Object for stock alert data presentation.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockAlertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long consumableId;
    private String consumableName;
    private String consumableCode;
    private String alertType;
    private String message;
    private Integer threshold;
    private Integer currentValue;
    private Boolean acknowledged;
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createTime;
}
