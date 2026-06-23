package com.zyy.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.asset.mapper.MaintenancePlanMapper;
import com.zyy.asset.model.entity.EquipmentEntity;
import com.zyy.asset.model.entity.MaintenancePlanEntity;
import com.zyy.asset.model.vo.AssetHealthScore;
import com.zyy.asset.service.AssetHealthService;
import com.zyy.audit.mapper.OperationLogMapper;
import com.zyy.audit.model.entity.OperationLogEntity;
import com.zyy.inventory.mapper.ConsumableMapper;
import com.zyy.inventory.mapper.InventoryTransactionMapper;
import com.zyy.inventory.model.entity.ConsumableEntity;
import com.zyy.inventory.model.entity.InventoryTransactionEntity;
import com.zyy.report.model.AssetSummaryReport;
import com.zyy.report.model.AuditTrailReport;
import com.zyy.report.model.InventorySummaryReport;
import com.zyy.report.model.MaintenanceHistoryReport;
import com.zyy.report.model.StockMovementReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of report generation service.
 * Aggregates data from existing mappers across modules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final EquipmentMapper equipmentMapper;
    private final MaintenancePlanMapper maintenancePlanMapper;
    private final ConsumableMapper consumableMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final OperationLogMapper operationLogMapper;
    private final AssetHealthService assetHealthService;

    @Override
    @Transactional(readOnly = true)
    public AssetSummaryReport generateAssetSummary() {
        List<EquipmentEntity> allEquipment = equipmentMapper.selectList(null);

        int total = allEquipment.size();

        // By status
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        byStatus.put("maintenance", 0);
        byStatus.put("normal", 0);
        byStatus.put("scrapped", 0);
        for (EquipmentEntity e : allEquipment) {
            String key = switch (e.getStatus()) {
                case 0 -> "maintenance";
                case 1 -> "normal";
                case 2 -> "scrapped";
                default -> "unknown";
            };
            byStatus.merge(key, 1, Integer::sum);
        }

        // By category
        Map<String, Integer> byCategory = allEquipment.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(EquipmentEntity::getCategory,
                        Collectors.summingInt(e -> 1)));

        // Average age
        double averageAge = allEquipment.stream()
                .filter(e -> e.getPurchaseDate() != null)
                .mapToLong(e -> ChronoUnit.YEARS.between(e.getPurchaseDate(), LocalDate.now()))
                .average()
                .orElse(0.0);

        // Overdue maintenance
        int overdueMaintenance = (int) allEquipment.stream()
                .filter(e -> e.getNextMaintenanceDate() != null
                        && e.getNextMaintenanceDate().isBefore(LocalDate.now())
                        && e.getStatus() != 2)
                .count();

        // Health distribution
        List<AssetHealthScore> healthScores = assetHealthService.getAllHealthScores();
        Map<String, Integer> healthDistribution = new LinkedHashMap<>();
        healthDistribution.put("A", 0);
        healthDistribution.put("B", 0);
        healthDistribution.put("C", 0);
        healthDistribution.put("D", 0);
        for (AssetHealthScore hs : healthScores) {
            healthDistribution.merge(hs.getGrade(), 1, Integer::sum);
        }

        return AssetSummaryReport.builder()
                .totalEquipment(total)
                .byStatus(byStatus)
                .byCategory(byCategory)
                .averageAge(Math.round(averageAge * 100.0) / 100.0)
                .overdueMaintenance(overdueMaintenance)
                .healthDistribution(healthDistribution)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryReport generateInventorySummary() {
        List<ConsumableEntity> allConsumables = consumableMapper.selectList(null);

        int totalConsumables = allConsumables.size();

        // Total value
        BigDecimal totalValue = allConsumables.stream()
                .filter(c -> c.getUnitCost() != null && c.getStockQuantity() != null)
                .map(c -> c.getUnitCost().multiply(BigDecimal.valueOf(c.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Low stock items
        int lowStockItems = (int) allConsumables.stream()
                .filter(c -> c.getStockQuantity() != null && c.getMinStockLevel() != null
                        && c.getStockQuantity() <= c.getMinStockLevel())
                .count();

        // Expiring items (within 30 days)
        int expiringItems = (int) allConsumables.stream()
                .filter(c -> c.getExpirationDate() != null
                        && !c.getExpirationDate().isAfter(LocalDate.now().plusDays(30)))
                .count();

        // By category
        Map<String, Integer> byCategory = allConsumables.stream()
                .filter(c -> c.getCategory() != null)
                .collect(Collectors.groupingBy(ConsumableEntity::getCategory,
                        Collectors.summingInt(c -> 1)));

        // Top suppliers
        List<String> topSuppliers = allConsumables.stream()
                .filter(c -> c.getSupplier() != null)
                .collect(Collectors.groupingBy(ConsumableEntity::getSupplier, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return InventorySummaryReport.builder()
                .totalConsumables(totalConsumables)
                .totalValue(totalValue.setScale(2, RoundingMode.HALF_UP))
                .lowStockItems(lowStockItems)
                .expiringItems(expiringItems)
                .byCategory(byCategory)
                .topSuppliers(topSuppliers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceHistoryReport generateMaintenanceHistory(LocalDateTime startTime, LocalDateTime endTime, Long equipmentId) {
        LambdaQueryWrapper<MaintenancePlanEntity> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(MaintenancePlanEntity::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(MaintenancePlanEntity::getCreateTime, endTime);
        }
        if (equipmentId != null) {
            wrapper.eq(MaintenancePlanEntity::getEquipmentId, equipmentId);
        }

        List<MaintenancePlanEntity> plans = maintenancePlanMapper.selectList(wrapper);

        int totalRecords = plans.size();

        // By type
        Map<String, Integer> byType = plans.stream()
                .filter(p -> p.getPlanType() != null)
                .collect(Collectors.groupingBy(MaintenancePlanEntity::getPlanType,
                        Collectors.summingInt(p -> 1)));

        // Average cost
        BigDecimal averageCost = plans.stream()
                .filter(p -> p.getCost() != null)
                .map(MaintenancePlanEntity::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long costCount = plans.stream().filter(p -> p.getCost() != null).count();
        if (costCount > 0) {
            averageCost = averageCost.divide(BigDecimal.valueOf(costCount), 2, RoundingMode.HALF_UP);
        }

        // By month
        Map<String, Integer> byMonth = new LinkedHashMap<>();
        for (MaintenancePlanEntity plan : plans) {
            if (plan.getCreateTime() != null) {
                String month = plan.getCreateTime().getYear() + "-"
                        + String.format("%02d", plan.getCreateTime().getMonthValue());
                byMonth.merge(month, 1, Integer::sum);
            }
        }

        // Top equipment by maintenance frequency
        List<String> topEquipment = plans.stream()
                .filter(p -> p.getEquipmentId() != null)
                .collect(Collectors.groupingBy(MaintenancePlanEntity::getEquipmentId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    EquipmentEntity eq = equipmentMapper.selectById(e.getKey());
                    return eq != null ? eq.getName() : "Equipment#" + e.getKey();
                })
                .collect(Collectors.toList());

        return MaintenanceHistoryReport.builder()
                .totalRecords(totalRecords)
                .byType(byType)
                .averageCost(averageCost)
                .byMonth(byMonth)
                .topEquipment(topEquipment)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditTrailReport generateAuditTrail(LocalDateTime startTime, LocalDateTime endTime, String module, Long userId) {
        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(OperationLogEntity::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLogEntity::getCreateTime, endTime);
        }
        if (module != null && !module.isBlank()) {
            wrapper.like(OperationLogEntity::getModule, module);
        }
        if (userId != null) {
            wrapper.eq(OperationLogEntity::getOperatorId, userId);
        }

        List<OperationLogEntity> logs = operationLogMapper.selectList(wrapper);

        int totalOperations = logs.size();

        // By module
        Map<String, Integer> byModule = logs.stream()
                .filter(l -> l.getModule() != null)
                .collect(Collectors.groupingBy(OperationLogEntity::getModule,
                        Collectors.summingInt(l -> 1)));

        // By operation
        Map<String, Integer> byOperation = logs.stream()
                .filter(l -> l.getOperation() != null)
                .collect(Collectors.groupingBy(OperationLogEntity::getOperation,
                        Collectors.summingInt(l -> 1)));

        // Top operators
        List<String> topOperators = logs.stream()
                .filter(l -> l.getOperatorName() != null)
                .collect(Collectors.groupingBy(OperationLogEntity::getOperatorName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Anomaly count (failed operations)
        int anomalyCount = (int) logs.stream()
                .filter(l -> l.getStatus() != null && l.getStatus() == 0)
                .count();

        return AuditTrailReport.builder()
                .totalOperations(totalOperations)
                .byModule(byModule)
                .byOperation(byOperation)
                .topOperators(topOperators)
                .anomalyCount(anomalyCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StockMovementReport generateStockMovement(LocalDateTime startTime, LocalDateTime endTime, Long consumableId) {
        LambdaQueryWrapper<InventoryTransactionEntity> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(InventoryTransactionEntity::getTransactionTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(InventoryTransactionEntity::getTransactionTime, endTime);
        }
        if (consumableId != null) {
            wrapper.eq(InventoryTransactionEntity::getConsumableId, consumableId);
        }

        List<InventoryTransactionEntity> transactions = inventoryTransactionMapper.selectList(wrapper);

        int totalTransactions = transactions.size();

        // Inbound/outbound totals
        int inboundTotal = transactions.stream()
                .filter(t -> "INBOUND".equals(t.getTransactionType()) && t.getQuantity() != null)
                .mapToInt(InventoryTransactionEntity::getQuantity)
                .sum();

        int outboundTotal = transactions.stream()
                .filter(t -> "OUTBOUND".equals(t.getTransactionType()) && t.getQuantity() != null)
                .mapToInt(InventoryTransactionEntity::getQuantity)
                .sum();

        // By type
        Map<String, Integer> byType = transactions.stream()
                .filter(t -> t.getTransactionType() != null)
                .collect(Collectors.groupingBy(InventoryTransactionEntity::getTransactionType,
                        Collectors.summingInt(t -> 1)));

        // By consumable (quantity)
        Map<String, Integer> byConsumable = new LinkedHashMap<>();
        transactions.stream()
                .filter(t -> t.getConsumableId() != null && t.getQuantity() != null)
                .collect(Collectors.groupingBy(InventoryTransactionEntity::getConsumableId,
                        Collectors.summingInt(InventoryTransactionEntity::getQuantity)))
                .forEach((id, qty) -> {
                    ConsumableEntity c = consumableMapper.selectById(id);
                    String name = c != null ? c.getName() : "Consumable#" + id;
                    byConsumable.put(name, qty);
                });

        // Top moved items
        List<String> topMovedItems = byConsumable.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return StockMovementReport.builder()
                .totalTransactions(totalTransactions)
                .inboundTotal(inboundTotal)
                .outboundTotal(outboundTotal)
                .byType(byType)
                .byConsumable(byConsumable)
                .topMovedItems(topMovedItems)
                .build();
    }
}
