package com.zyy.report.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Stock movement report model.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockMovementReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total number of transactions */
    private Integer totalTransactions;

    /** Total inbound quantity */
    private Integer inboundTotal;

    /** Total outbound quantity */
    private Integer outboundTotal;

    /** Transaction count by type */
    private Map<String, Integer> byType;

    /** Transaction quantity by consumable */
    private Map<String, Integer> byConsumable;

    /** Top moved items by quantity */
    private List<String> topMovedItems;
}
