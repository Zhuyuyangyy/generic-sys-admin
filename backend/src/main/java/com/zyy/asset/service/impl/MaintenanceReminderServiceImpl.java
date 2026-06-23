package com.zyy.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.asset.model.entity.EquipmentEntity;
import com.zyy.asset.model.vo.EquipmentVO;
import com.zyy.asset.service.MaintenanceReminderService;
import com.zyy.websocket.EventWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of maintenance reminder business service.
 * <p>
 * Queries equipment for overdue and upcoming maintenance schedules,
 * and broadcasts WebSocket alerts when maintenance checks are triggered.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceReminderServiceImpl implements MaintenanceReminderService {

    private final EquipmentMapper equipmentMapper;

    @Override
    public List<EquipmentVO> getOverdueMaintenance() {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<EquipmentEntity> query = new LambdaQueryWrapper<>();
        query.eq(EquipmentEntity::getIsDeleted, 0)
             .lt(EquipmentEntity::getNextMaintenanceDate, today)
             .ne(EquipmentEntity::getStatus, 2); // Exclude scrapped

        List<EquipmentEntity> entities = equipmentMapper.selectList(query);
        return entities.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EquipmentVO> getUpcomingMaintenance(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);

        LambdaQueryWrapper<EquipmentEntity> query = new LambdaQueryWrapper<>();
        query.eq(EquipmentEntity::getIsDeleted, 0)
             .ne(EquipmentEntity::getStatus, 2)
             .ge(EquipmentEntity::getNextMaintenanceDate, today)
             .le(EquipmentEntity::getNextMaintenanceDate, deadline);

        List<EquipmentEntity> entities = equipmentMapper.selectList(query);
        return entities.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndAlert() {
        List<EquipmentVO> overdue = getOverdueMaintenance();
        List<EquipmentVO> upcoming = getUpcomingMaintenance(7);

        // Broadcast overdue maintenance alerts
        for (EquipmentVO equipment : overdue) {
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("equipmentId", equipment.getId());
            alertData.put("equipmentCode", equipment.getEquipmentCode());
            alertData.put("equipmentName", equipment.getName());
            alertData.put("nextMaintenanceDate", equipment.getNextMaintenanceDate());
            alertData.put("alertType", "OVERDUE");

            EventWebSocket.broadcastEvent("MAINTENANCE_OVERDUE", alertData);
            log.warn("Maintenance overdue alert - equipmentId={}, name={}, nextMaintenanceDate={}",
                    equipment.getId(), equipment.getName(), equipment.getNextMaintenanceDate());
        }

        // Broadcast upcoming maintenance alerts
        for (EquipmentVO equipment : upcoming) {
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("equipmentId", equipment.getId());
            alertData.put("equipmentCode", equipment.getEquipmentCode());
            alertData.put("equipmentName", equipment.getName());
            alertData.put("nextMaintenanceDate", equipment.getNextMaintenanceDate());
            alertData.put("alertType", "UPCOMING");

            EventWebSocket.broadcastEvent("MAINTENANCE_UPCOMING", alertData);
        }

        log.info("Maintenance check completed - overdue={}, upcoming={}", overdue.size(), upcoming.size());
    }

    // ==================== Private Helper Methods ====================

    private EquipmentVO entityToVO(EquipmentEntity entity) {
        if (entity == null) return null;

        LocalDate today = LocalDate.now();
        boolean maintenanceOverdue = entity.getNextMaintenanceDate() != null
                && entity.getNextMaintenanceDate().isBefore(today)
                && entity.getStatus() != 2;
        boolean warrantyExpired = entity.getWarrantyExpiry() != null
                && entity.getWarrantyExpiry().isBefore(today);

        String statusText = switch (entity.getStatus()) {
            case 0 -> "Under Maintenance";
            case 1 -> "Normal";
            case 2 -> "Scrapped";
            default -> "Unknown";
        };

        return EquipmentVO.builder()
                .id(entity.getId())
                .equipmentCode(entity.getEquipmentCode())
                .name(entity.getName())
                .category(entity.getCategory())
                .model(entity.getModel())
                .manufacturer(entity.getManufacturer())
                .purchaseDate(entity.getPurchaseDate())
                .warrantyExpiry(entity.getWarrantyExpiry())
                .status(entity.getStatus())
                .statusText(statusText)
                .location(entity.getLocation())
                .maintenanceCycleDays(entity.getMaintenanceCycleDays())
                .nextMaintenanceDate(entity.getNextMaintenanceDate())
                .maintenanceOverdue(maintenanceOverdue)
                .warrantyExpired(warrantyExpired)
                .remarks(entity.getRemarks())
                .createTime(entity.getCreateTime())
                .build();
    }
}
