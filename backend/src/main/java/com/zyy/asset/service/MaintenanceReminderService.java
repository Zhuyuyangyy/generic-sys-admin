package com.zyy.asset.service;

import com.zyy.asset.model.vo.EquipmentVO;

import java.util.List;

/**
 * Maintenance reminder business service interface.
 * <p>
 * Defines operations for monitoring equipment maintenance schedules
 * and generating overdue / upcoming maintenance alerts.
 *
 * @author System Architect
 */
public interface MaintenanceReminderService {

    /**
     * Get equipment with overdue maintenance (nextMaintenanceDate before today).
     *
     * @return List of equipment with overdue maintenance
     */
    List<EquipmentVO> getOverdueMaintenance();

    /**
     * Get equipment with upcoming maintenance within N days.
     *
     * @param days Number of days to look ahead
     * @return List of equipment due for maintenance within the specified days
     */
    List<EquipmentVO> getUpcomingMaintenance(int days);

    /**
     * Check all equipment and generate alerts for overdue / upcoming maintenance.
     * Broadcasts WebSocket events for each overdue equipment found.
     */
    void checkAndAlert();
}
