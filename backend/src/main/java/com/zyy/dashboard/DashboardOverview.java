package com.zyy.dashboard;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Overview statistics DTO for the dashboard.
 *
 * @author System Architect
 */
@Data
@Builder
public class DashboardOverview implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalEquipment;
    private Long normalEquipment;
    private Long maintenanceEquipment;
    private Long scrappedEquipment;
    private Long totalConsumables;
    private Long lowStockCount;
    private Long pendingMaintenance;
}
