package com.zyy.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.asset.mapper.MaintenancePlanMapper;
import com.zyy.asset.model.entity.EquipmentEntity;
import com.zyy.asset.model.entity.MaintenancePlanEntity;
import com.zyy.inventory.mapper.ConsumableMapper;
import com.zyy.inventory.mapper.StockAlertMapper;
import com.zyy.inventory.model.entity.ConsumableEntity;
import com.zyy.inventory.model.entity.StockAlertEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of dashboard business service.
 * <p>
 * Aggregates data from equipment, consumable, maintenance plan,
 * and stock alert modules to provide dashboard statistics.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EquipmentMapper equipmentMapper;
    private final ConsumableMapper consumableMapper;
    private final MaintenancePlanMapper maintenancePlanMapper;
    private final StockAlertMapper stockAlertMapper;

    @Override
    public DashboardOverview getOverview() {
        long totalEquipment = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getIsDeleted, 0));
        long normalEquipment = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getIsDeleted, 0)
                        .eq(EquipmentEntity::getStatus, 1));
        long maintenanceEquipment = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getIsDeleted, 0)
                        .eq(EquipmentEntity::getStatus, 0));
        long scrappedEquipment = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getIsDeleted, 0)
                        .eq(EquipmentEntity::getStatus, 2));

        long totalConsumables = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0)
                        .eq(ConsumableEntity::getStatus, 1));

        long lowStockCount = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0)
                        .apply("stock_quantity < min_stock_level"));

        long pendingMaintenance = maintenancePlanMapper.selectCount(
                new LambdaQueryWrapper<MaintenancePlanEntity>()
                        .eq(MaintenancePlanEntity::getIsDeleted, 0)
                        .in(MaintenancePlanEntity::getStatus, "PENDING", "IN_PROGRESS"));

        return DashboardOverview.builder()
                .totalEquipment(totalEquipment)
                .normalEquipment(normalEquipment)
                .maintenanceEquipment(maintenanceEquipment)
                .scrappedEquipment(scrappedEquipment)
                .totalConsumables(totalConsumables)
                .lowStockCount(lowStockCount)
                .pendingMaintenance(pendingMaintenance)
                .build();
    }

    @Override
    public EquipmentStatusDistribution getEquipmentStatusDistribution() {
        long total = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getIsDeleted, 0));
        long normal = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getIsDeleted, 0)
                        .eq(EquipmentEntity::getStatus, 1));
        long maintenance = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getIsDeleted, 0)
                        .eq(EquipmentEntity::getStatus, 0));
        long scrapped = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getIsDeleted, 0)
                        .eq(EquipmentEntity::getStatus, 2));

        double normalPct = total > 0 ? (normal * 100.0 / total) : 0.0;
        double maintenancePct = total > 0 ? (maintenance * 100.0 / total) : 0.0;
        double scrappedPct = total > 0 ? (scrapped * 100.0 / total) : 0.0;

        return EquipmentStatusDistribution.builder()
                .normalCount(normal)
                .maintenanceCount(maintenance)
                .scrappedCount(scrapped)
                .normalPercentage(Math.round(normalPct * 100.0) / 100.0)
                .maintenancePercentage(Math.round(maintenancePct * 100.0) / 100.0)
                .scrappedPercentage(Math.round(scrappedPct * 100.0) / 100.0)
                .build();
    }

    @Override
    public InventorySummary getInventorySummary() {
        long totalConsumables = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0));
        long availableConsumables = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0)
                        .eq(ConsumableEntity::getStatus, 1));
        long lowStockCount = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0)
                        .apply("stock_quantity < min_stock_level"));
        long expiringCount = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0)
                        .isNotNull(ConsumableEntity::getExpirationDate)
                        .apply("expiration_date <= DATE_ADD(NOW(), INTERVAL 30 DAY)"));
        long overstockCount = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0)
                        .isNotNull(ConsumableEntity::getMaxStockLevel)
                        .apply("stock_quantity > max_stock_level"));

        // Calculate total inventory value
        List<ConsumableEntity> allConsumables = consumableMapper.selectList(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getIsDeleted, 0));
        BigDecimal totalValue = allConsumables.stream()
                .filter(c -> c.getUnitCost() != null && c.getStockQuantity() != null)
                .map(c -> c.getUnitCost().multiply(BigDecimal.valueOf(c.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InventorySummary.builder()
                .totalConsumables(totalConsumables)
                .availableConsumables(availableConsumables)
                .lowStockCount(lowStockCount)
                .expiringCount(expiringCount)
                .overstockCount(overstockCount)
                .totalInventoryValue(totalValue)
                .build();
    }

    @Override
    public List<RecentActivity> getRecentActivities(int limit) {
        List<RecentActivity> activities = new ArrayList<>();

        // Recent equipment registrations
        List<EquipmentEntity> recentEquipment = equipmentMapper.selectList(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getIsDeleted, 0)
                        .orderByDesc(EquipmentEntity::getCreateTime)
                        .last("LIMIT " + limit));
        for (EquipmentEntity e : recentEquipment) {
            activities.add(RecentActivity.builder()
                    .activityType("EQUIPMENT_REGISTERED")
                    .description("Equipment registered: " + e.getName())
                    .referenceId(e.getId())
                    .referenceType("EQUIPMENT")
                    .activityTime(e.getCreateTime())
                    .build());
        }

        // Recent maintenance plans
        List<MaintenancePlanEntity> recentPlans = maintenancePlanMapper.selectList(
                new LambdaQueryWrapper<MaintenancePlanEntity>()
                        .eq(MaintenancePlanEntity::getIsDeleted, 0)
                        .orderByDesc(MaintenancePlanEntity::getCreateTime)
                        .last("LIMIT " + limit));
        for (MaintenancePlanEntity p : recentPlans) {
            activities.add(RecentActivity.builder()
                    .activityType("MAINTENANCE_PLAN_CREATED")
                    .description("Maintenance plan created: " + p.getPlanName())
                    .referenceId(p.getId())
                    .referenceType("MAINTENANCE_PLAN")
                    .activityTime(p.getCreateTime())
                    .build());
        }

        // Recent stock alerts
        List<StockAlertEntity> recentAlerts = stockAlertMapper.selectList(
                new LambdaQueryWrapper<StockAlertEntity>()
                        .eq(StockAlertEntity::getIsDeleted, 0)
                        .orderByDesc(StockAlertEntity::getCreateTime)
                        .last("LIMIT " + limit));
        for (StockAlertEntity a : recentAlerts) {
            activities.add(RecentActivity.builder()
                    .activityType("STOCK_ALERT")
                    .description("Stock alert: " + a.getMessage())
                    .referenceId(a.getId())
                    .referenceType("STOCK_ALERT")
                    .activityTime(a.getCreateTime())
                    .build());
        }

        // Sort by time descending and limit
        return activities.stream()
                .sorted(Comparator.comparing(RecentActivity::getActivityTime).reversed())
                .limit(limit)
                .toList();
    }
}
