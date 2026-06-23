package com.zyy.dashboard;

import java.util.List;

/**
 * Dashboard business service interface.
 * <p>
 * Defines operations for dashboard statistics and overview data.
 *
 * @author System Architect
 */
public interface DashboardService {

    /**
     * Get overview statistics.
     *
     * @return Overview statistics
     */
    DashboardOverview getOverview();

    /**
     * Get equipment status distribution.
     *
     * @return Equipment status distribution data
     */
    EquipmentStatusDistribution getEquipmentStatusDistribution();

    /**
     * Get inventory summary.
     *
     * @return Inventory summary data
     */
    InventorySummary getInventorySummary();

    /**
     * Get recent activity logs.
     *
     * @param limit Maximum number of activities to return
     * @return List of recent activities
     */
    List<RecentActivity> getRecentActivities(int limit);
}
