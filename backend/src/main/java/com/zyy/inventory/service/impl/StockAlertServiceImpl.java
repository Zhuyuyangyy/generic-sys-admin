package com.zyy.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.inventory.mapper.ConsumableMapper;
import com.zyy.inventory.mapper.StockAlertMapper;
import com.zyy.inventory.model.entity.ConsumableEntity;
import com.zyy.inventory.model.entity.StockAlertEntity;
import com.zyy.inventory.model.vo.StockAlertVO;
import com.zyy.common.PageVO;
import com.zyy.inventory.service.StockAlertService;
import com.zyy.websocket.EventWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of stock alert business service.
 * <p>
 * Provides alert monitoring, summary statistics, and acknowledgment workflows.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertServiceImpl implements StockAlertService {

    private final StockAlertMapper stockAlertMapper;
    private final ConsumableMapper consumableMapper;

    @Override
    public PageVO<StockAlertVO> getPage(Long pageNum, Long pageSize, String alertType) {
        Page<StockAlertEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StockAlertEntity> query = new LambdaQueryWrapper<>();
        query.eq(StockAlertEntity::getIsDeleted, 0);

        if (alertType != null && !alertType.isBlank()) {
            query.eq(StockAlertEntity::getAlertType, alertType);
        }

        query.orderByDesc(StockAlertEntity::getCreateTime);
        Page<StockAlertEntity> result = stockAlertMapper.selectPage(page, query);

        List<StockAlertVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<StockAlertVO>builder()
                .items(voList)
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .isFirst(result.getCurrent() == 1)
                .isLast(result.getCurrent() >= result.getPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
    }

    @Override
    public Map<String, Long> getSummary() {
        LambdaQueryWrapper<StockAlertEntity> query = new LambdaQueryWrapper<>();
        query.eq(StockAlertEntity::getIsDeleted, 0)
             .eq(StockAlertEntity::getAcknowledged, false);

        List<StockAlertEntity> unacknowledged = stockAlertMapper.selectList(query);

        Map<String, Long> summary = new HashMap<>();
        summary.put("LOW_STOCK", 0L);
        summary.put("EXPIRING", 0L);
        summary.put("OVERSTOCK", 0L);
        summary.put("TOTAL", (long) unacknowledged.size());

        for (StockAlertEntity alert : unacknowledged) {
            String type = alert.getAlertType();
            summary.merge(type, 1L, Long::sum);
        }

        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledge(Long id, Long operatorId) {
        StockAlertEntity entity = stockAlertMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Stock alert not found: " + id);
        }

        if (Boolean.TRUE.equals(entity.getAcknowledged())) {
            throw new BusinessException("Alert already acknowledged");
        }

        entity.setAcknowledged(true);
        entity.setAcknowledgedBy(operatorId);
        entity.setAcknowledgedAt(LocalDateTime.now());

        stockAlertMapper.updateById(entity);
        log.info("Stock alert acknowledged - id={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndCreateAlerts(ConsumableEntity entity) {
        LocalDate today = LocalDate.now();

        // LOW_STOCK: stock below minStockLevel
        if (entity.getMinStockLevel() != null && entity.getStockQuantity() < entity.getMinStockLevel()) {
            createAndBroadcastAlert(entity, "LOW_STOCK",
                    String.format("Low stock: %s (current: %d, min: %d)",
                            entity.getName(), entity.getStockQuantity(), entity.getMinStockLevel()),
                    entity.getMinStockLevel(), entity.getStockQuantity());
        }

        // EXPIRING: expiration within 30 days
        if (entity.getExpirationDate() != null
                && !entity.getExpirationDate().isBefore(today)
                && !entity.getExpirationDate().isAfter(today.plusDays(30))) {
            createAndBroadcastAlert(entity, "EXPIRING",
                    String.format("Expiring soon: %s (expires: %s)",
                            entity.getName(), entity.getExpirationDate()),
                    30, (int) java.time.temporal.ChronoUnit.DAYS.between(today, entity.getExpirationDate()));
        }

        // OVERSTOCK: stock above maxStockLevel
        if (entity.getMaxStockLevel() != null && entity.getStockQuantity() > entity.getMaxStockLevel()) {
            createAndBroadcastAlert(entity, "OVERSTOCK",
                    String.format("Overstock: %s (current: %d, max: %d)",
                            entity.getName(), entity.getStockQuantity(), entity.getMaxStockLevel()),
                    entity.getMaxStockLevel(), entity.getStockQuantity());
        }
    }

    // ==================== Private Helper Methods ====================

    private void createAndBroadcastAlert(ConsumableEntity entity, String alertType,
                                          String message, Integer threshold, Integer currentValue) {
        // Check if an unacknowledged alert of the same type already exists for this consumable
        LambdaQueryWrapper<StockAlertEntity> existingQuery = new LambdaQueryWrapper<>();
        existingQuery.eq(StockAlertEntity::getConsumableId, entity.getId())
                     .eq(StockAlertEntity::getAlertType, alertType)
                     .eq(StockAlertEntity::getAcknowledged, false)
                     .eq(StockAlertEntity::getIsDeleted, 0);
        long existingCount = stockAlertMapper.selectCount(existingQuery);
        if (existingCount > 0) {
            log.debug("Active {} alert already exists for consumableId={}", alertType, entity.getId());
            return;
        }

        StockAlertEntity alert = new StockAlertEntity();
        alert.setConsumableId(entity.getId());
        alert.setAlertType(alertType);
        alert.setMessage(message);
        alert.setThreshold(threshold);
        alert.setCurrentValue(currentValue);
        alert.setAcknowledged(false);
        stockAlertMapper.insert(alert);

        // Broadcast via WebSocket
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("alertId", alert.getId());
        alertData.put("consumableId", entity.getId());
        alertData.put("consumableName", entity.getName());
        alertData.put("consumableCode", entity.getProductCode());
        alertData.put("alertType", alertType);
        alertData.put("message", message);
        alertData.put("threshold", threshold);
        alertData.put("currentValue", currentValue);

        EventWebSocket.broadcastEvent("STOCK_ALERT", alertData);

        log.info("Stock alert created and broadcast - type={}, consumableId={}, name={}",
                alertType, entity.getId(), entity.getName());
    }

    private StockAlertVO entityToVO(StockAlertEntity entity) {
        if (entity == null) return null;

        // Resolve consumable info
        String consumableName = null;
        String consumableCode = null;
        if (entity.getConsumableId() != null) {
            ConsumableEntity consumable = consumableMapper.selectById(entity.getConsumableId());
            if (consumable != null) {
                consumableName = consumable.getName();
                consumableCode = consumable.getProductCode();
            }
        }

        return StockAlertVO.builder()
                .id(entity.getId())
                .consumableId(entity.getConsumableId())
                .consumableName(consumableName)
                .consumableCode(consumableCode)
                .alertType(entity.getAlertType())
                .message(entity.getMessage())
                .threshold(entity.getThreshold())
                .currentValue(entity.getCurrentValue())
                .acknowledged(entity.getAcknowledged())
                .acknowledgedBy(entity.getAcknowledgedBy())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .createTime(entity.getCreateTime())
                .build();
    }
}
