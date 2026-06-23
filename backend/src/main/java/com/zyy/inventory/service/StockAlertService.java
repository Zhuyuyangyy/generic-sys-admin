package com.zyy.inventory.service;

import com.zyy.inventory.model.entity.ConsumableEntity;
import com.zyy.inventory.model.vo.StockAlertVO;
import com.zyy.common.PageVO;

import java.util.Map;

/**
 * Stock alert business service interface.
 * <p>
 * Defines operations for monitoring and acknowledging inventory alerts.
 *
 * @author System Architect
 */
public interface StockAlertService {

    /**
     * Retrieve paginated active alerts with optional type filter.
     *
     * @param pageNum   Page number (1-based)
     * @param pageSize  Items per page
     * @param alertType Optional alert type filter: LOW_STOCK, EXPIRING, OVERSTOCK
     * @return Paginated alert list
     */
    PageVO<StockAlertVO> getPage(Long pageNum, Long pageSize, String alertType);

    /**
     * Get alert summary counts grouped by type.
     *
     * @return Map of alert type to count
     */
    Map<String, Long> getSummary();

    /**
     * Acknowledge an alert.
     *
     * @param id          Alert identifier
     * @param operatorId  User ID acknowledging the alert
     */
    void acknowledge(Long id, Long operatorId);

    /**
     * Check a consumable entity and create alerts if thresholds are breached.
     * <p>
     * Generates alerts for:
     * <ul>
     *   <li>LOW_STOCK  - stock below minStockLevel</li>
     *   <li>EXPIRING   - expiration within 30 days</li>
     *   <li>OVERSTOCK  - stock above maxStockLevel</li>
     * </ul>
     * Each created alert is broadcast via WebSocket.
     *
     * @param entity The consumable entity to check
     */
    void checkAndCreateAlerts(ConsumableEntity entity);
}
