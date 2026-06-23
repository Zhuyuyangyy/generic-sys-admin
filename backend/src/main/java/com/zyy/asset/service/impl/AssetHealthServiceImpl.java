package com.zyy.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.asset.mapper.MaintenancePlanMapper;
import com.zyy.asset.model.entity.EquipmentEntity;
import com.zyy.asset.model.entity.MaintenancePlanEntity;
import com.zyy.asset.model.vo.AssetHealthOverview;
import com.zyy.asset.model.vo.AssetHealthScore;
import com.zyy.asset.service.AssetHealthService;
import com.zyy.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of asset health scoring service.
 * <p>
 * Health score calculation (0-100):
 * <ul>
 *   <li>Base score: 100</li>
 *   <li>Maintenance overdue: -30</li>
 *   <li>Warranty expired: -10</li>
 *   <li>No maintenance in 180+ days: -20</li>
 *   <li>Status is maintenance: -15</li>
 *   <li>Recent anomaly found: -10</li>
 * </ul>
 * Health grade: A (90-100), B (70-89), C (50-69), D (0-49)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetHealthServiceImpl implements AssetHealthService {

    private final EquipmentMapper equipmentMapper;
    private final MaintenancePlanMapper maintenancePlanMapper;

    private static final int DEDUCTION_MAINTENANCE_OVERDUE = 30;
    private static final int DEDUCTION_WARRANTY_EXPIRED = 10;
    private static final int DEDUCTION_NO_MAINTENANCE_180 = 20;
    private static final int DEDUCTION_STATUS_MAINTENANCE = 15;
    private static final int DEDUCTION_RECENT_ANOMALY = 10;
    private static final long NO_MAINTENANCE_THRESHOLD_DAYS = 180;

    @Override
    @Transactional(readOnly = true)
    public AssetHealthScore calculateHealthScore(Long equipmentId) {
        EquipmentEntity equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException("Equipment not found: " + equipmentId);
        }

        return buildHealthScore(equipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetHealthScore> getAllHealthScores() {
        LambdaQueryWrapper<EquipmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(EquipmentEntity::getStatus, 2); // Exclude scrapped equipment

        List<EquipmentEntity> equipmentList = equipmentMapper.selectList(wrapper);
        return equipmentList.stream()
                .map(this::buildHealthScore)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetHealthOverview getHealthOverview() {
        List<AssetHealthScore> scores = getAllHealthScores();

        double averageScore = scores.stream()
                .mapToInt(AssetHealthScore::getScore)
                .average()
                .orElse(0.0);

        Map<String, Integer> gradeDistribution = new LinkedHashMap<>();
        gradeDistribution.put("A", 0);
        gradeDistribution.put("B", 0);
        gradeDistribution.put("C", 0);
        gradeDistribution.put("D", 0);

        int atRiskCount = 0;
        int criticalCount = 0;

        for (AssetHealthScore score : scores) {
            String grade = score.getGrade();
            gradeDistribution.merge(grade, 1, Integer::sum);
            if ("C".equals(grade) || "D".equals(grade)) {
                atRiskCount++;
            }
            if ("D".equals(grade)) {
                criticalCount++;
            }
        }

        return AssetHealthOverview.builder()
                .totalEquipment(scores.size())
                .averageScore(Math.round(averageScore * 100.0) / 100.0)
                .gradeDistribution(gradeDistribution)
                .atRiskCount(atRiskCount)
                .criticalCount(criticalCount)
                .build();
    }

    /**
     * Build the health score for a single equipment entity.
     */
    private AssetHealthScore buildHealthScore(EquipmentEntity equipment) {
        int score = 100;
        List<String> factors = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Check maintenance overdue
        boolean isOverdue = false;
        if (equipment.getNextMaintenanceDate() != null && equipment.getNextMaintenanceDate().isBefore(today)) {
            score -= DEDUCTION_MAINTENANCE_OVERDUE;
            factors.add("Maintenance overdue by " + ChronoUnit.DAYS.between(equipment.getNextMaintenanceDate(), today) + " days (-" + DEDUCTION_MAINTENANCE_OVERDUE + ")");
            isOverdue = true;
        }

        // Check warranty expired
        boolean warrantyExpired = false;
        if (equipment.getWarrantyExpiry() != null && equipment.getWarrantyExpiry().isBefore(today)) {
            score -= DEDUCTION_WARRANTY_EXPIRED;
            factors.add("Warranty expired on " + equipment.getWarrantyExpiry() + " (-" + DEDUCTION_WARRANTY_EXPIRED + ")");
            warrantyExpired = true;
        }

        // Check no maintenance in 180+ days
        LocalDate lastMaintenanceDate = findLastMaintenanceDate(equipment.getId());
        if (lastMaintenanceDate != null) {
            long daysSinceMaintenance = ChronoUnit.DAYS.between(lastMaintenanceDate, today);
            if (daysSinceMaintenance > NO_MAINTENANCE_THRESHOLD_DAYS) {
                score -= DEDUCTION_NO_MAINTENANCE_180;
                factors.add("No maintenance in " + daysSinceMaintenance + " days (-" + DEDUCTION_NO_MAINTENANCE_180 + ")");
            }
        }

        // Check status is maintenance (0 = maintenance)
        if (equipment.getStatus() != null && equipment.getStatus() == 0) {
            score -= DEDUCTION_STATUS_MAINTENANCE;
            factors.add("Equipment currently under maintenance (-" + DEDUCTION_STATUS_MAINTENANCE + ")");
        }

        // Check recent anomaly from inventory records
        if (hasRecentAnomaly(equipment.getId())) {
            score -= DEDUCTION_RECENT_ANOMALY;
            factors.add("Recent anomaly detected in inspection records (-" + DEDUCTION_RECENT_ANOMALY + ")");
        }

        // Clamp score to 0 minimum
        score = Math.max(0, score);

        if (factors.isEmpty()) {
            factors.add("All health checks passed");
        }

        return AssetHealthScore.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getName())
                .score(score)
                .grade(toGrade(score))
                .factors(factors)
                .lastMaintenanceDate(lastMaintenanceDate)
                .nextMaintenanceDate(equipment.getNextMaintenanceDate())
                .isOverdue(isOverdue)
                .warrantyExpired(warrantyExpired)
                .build();
    }

    /**
     * Find the most recent completed maintenance date for an equipment.
     */
    private LocalDate findLastMaintenanceDate(Long equipmentId) {
        LambdaQueryWrapper<MaintenancePlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenancePlanEntity::getEquipmentId, equipmentId)
                .eq(MaintenancePlanEntity::getStatus, "COMPLETED")
                .orderByDesc(MaintenancePlanEntity::getCompletedDate)
                .last("LIMIT 1");

        MaintenancePlanEntity plan = maintenancePlanMapper.selectOne(wrapper);
        return plan != null ? plan.getCompletedDate() : null;
    }

    /**
     * Check if the equipment has a recent anomaly in inventory records.
     * Status 2 = anomaly found.
     */
    private boolean hasRecentAnomaly(Long equipmentId) {
        LambdaQueryWrapper<MaintenancePlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenancePlanEntity::getEquipmentId, equipmentId)
                .eq(MaintenancePlanEntity::getStatus, "COMPLETED")
                .ge(MaintenancePlanEntity::getCreateTime, LocalDateTime.now().minusDays(30))
                .like(MaintenancePlanEntity::getDescription, "anomaly");

        return maintenancePlanMapper.selectCount(wrapper) > 0;
    }

    /**
     * Convert a numeric score to a health grade.
     */
    private String toGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 70) return "B";
        if (score >= 50) return "C";
        return "D";
    }
}
