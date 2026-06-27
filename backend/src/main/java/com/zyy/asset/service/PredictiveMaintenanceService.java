package com.zyy.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.asset.mapper.MaintenancePlanMapper;
import com.zyy.asset.model.entity.EquipmentEntity;
import com.zyy.asset.model.entity.MaintenancePlanEntity;
import com.zyy.asset.model.vo.MaintenancePredictionVO;
import com.zyy.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Predictive maintenance service for equipment assets.
 * <p>
 * Predicts next maintenance based on:
 * <ul>
 *   <li>Maintenance cycle (configured interval in days)</li>
 *   <li>Last maintenance date</li>
 *   <li>Equipment age</li>
 *   <li>Recent failure rate (emergency/corrective maintenance count)</li>
 * </ul>
 * <p>
 * Risk assessment:
 * <ul>
 *   <li>maintenanceRisk: probability of missing next scheduled maintenance</li>
 *   <li>riskLevel: overall risk based on multiple factors</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictiveMaintenanceService {

    private final EquipmentMapper equipmentMapper;
    private final MaintenancePlanMapper maintenancePlanMapper;

    /** Default maintenance cycle if not configured on equipment */
    private static final int DEFAULT_CYCLE_DAYS = 90;

    @Transactional(readOnly = true)
    public MaintenancePredictionVO predictNextMaintenance(Long equipmentId) {
        EquipmentEntity equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException("Equipment not found: " + equipmentId);
        }

        return buildPrediction(equipment);
    }

    @Transactional(readOnly = true)
    public List<MaintenancePredictionVO> getAllMaintenancePredictions() {
        LambdaQueryWrapper<EquipmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(EquipmentEntity::getStatus, 2); // Exclude scrapped

        List<EquipmentEntity> equipmentList = equipmentMapper.selectList(wrapper);
        return equipmentList.stream()
                .map(this::buildPrediction)
                .collect(Collectors.toList());
    }

    private MaintenancePredictionVO buildPrediction(EquipmentEntity equipment) {
        LocalDate today = LocalDate.now();

        // Get last maintenance date
        LocalDate lastMaintenanceDate = findLastMaintenanceDate(equipment.getId());

        // Get maintenance cycle
        int cycleDays = equipment.getMaintenanceCycleDays() != null
                ? equipment.getMaintenanceCycleDays() : DEFAULT_CYCLE_DAYS;

        // Calculate predicted next maintenance
        LocalDate predictedNextDate;
        if (lastMaintenanceDate != null) {
            predictedNextDate = lastMaintenanceDate.plusDays(cycleDays);
        } else if (equipment.getNextMaintenanceDate() != null) {
            predictedNextDate = equipment.getNextMaintenanceDate();
        } else if (equipment.getPurchaseDate() != null) {
            // No maintenance history - predict from purchase date
            long ageDays = ChronoUnit.DAYS.between(equipment.getPurchaseDate(), today);
            long completedCycles = ageDays / cycleDays;
            predictedNextDate = equipment.getPurchaseDate().plusDays((completedCycles + 1) * cycleDays);
        } else {
            predictedNextDate = today.plusDays(cycleDays);
        }

        // Days since last maintenance
        Long daysSinceLastMaintenance = lastMaintenanceDate != null
                ? ChronoUnit.DAYS.between(lastMaintenanceDate, today) : null;

        // Days until next maintenance
        Long daysUntilNextMaintenance = ChronoUnit.DAYS.between(today, predictedNextDate);

        // Equipment age
        Long equipmentAgeDays = equipment.getPurchaseDate() != null
                ? ChronoUnit.DAYS.between(equipment.getPurchaseDate(), today) : null;

        // Failure rate: emergency/corrective maintenance in last 180 days
        int recentFailureCount = countRecentFailures(equipment.getId());

        // Calculate maintenance risk
        String maintenanceRisk = calculateMaintenanceRisk(
                daysUntilNextMaintenance, cycleDays, recentFailureCount, equipmentAgeDays);

        // Overall risk level
        String riskLevel = calculateOverallRisk(
                daysUntilNextMaintenance, recentFailureCount, equipmentAgeDays, cycleDays);

        // Build reasoning
        List<String> reasoning = buildReasoning(
                lastMaintenanceDate, predictedNextDate, daysSinceLastMaintenance,
                daysUntilNextMaintenance, cycleDays, recentFailureCount, equipmentAgeDays);

        return MaintenancePredictionVO.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getName())
                .category(equipment.getCategory())
                .status(equipment.getStatus())
                .lastMaintenanceDate(lastMaintenanceDate)
                .nextMaintenanceDate(equipment.getNextMaintenanceDate())
                .predictedNextMaintenanceDate(predictedNextDate)
                .maintenanceCycleDays(cycleDays)
                .daysSinceLastMaintenance(daysSinceLastMaintenance)
                .daysUntilNextMaintenance(daysUntilNextMaintenance)
                .equipmentAgeDays(equipmentAgeDays)
                .recentFailureCount(recentFailureCount)
                .maintenanceRisk(maintenanceRisk)
                .riskLevel(riskLevel)
                .reasoning(reasoning)
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
     * Count emergency and corrective maintenance plans in the last 180 days.
     */
    private int countRecentFailures(Long equipmentId) {
        LambdaQueryWrapper<MaintenancePlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenancePlanEntity::getEquipmentId, equipmentId)
                .in(MaintenancePlanEntity::getPlanType, "EMERGENCY", "CORRECTIVE")
                .ge(MaintenancePlanEntity::getCreateTime, LocalDateTime.now().minusDays(180));

        return (int) maintenancePlanMapper.selectCount(wrapper);
    }

    /**
     * Calculate risk of missing next scheduled maintenance.
     */
    private String calculateMaintenanceRisk(Long daysUntilNext, int cycleDays,
                                             int failureCount, Long ageDays) {
        // Higher risk if: close to or past maintenance date, frequent failures, old equipment
        double riskScore = 0;

        // Time pressure factor
        if (daysUntilNext < 0) {
            riskScore += 40; // Already overdue
        } else if (daysUntilNext < 7) {
            riskScore += 30;
        } else if (daysUntilNext < 14) {
            riskScore += 15;
        }

        // Failure history factor
        riskScore += Math.min(failureCount * 10, 30);

        // Age factor (older equipment is harder to maintain on schedule)
        if (ageDays != null && ageDays > 365 * 5) {
            riskScore += 15;
        } else if (ageDays != null && ageDays > 365 * 3) {
            riskScore += 10;
        }

        if (riskScore >= 60) return "CRITICAL";
        if (riskScore >= 40) return "HIGH";
        if (riskScore >= 20) return "MEDIUM";
        return "LOW";
    }

    /**
     * Calculate overall risk level combining all factors.
     */
    private String calculateOverallRisk(Long daysUntilNext, int failureCount,
                                         Long ageDays, int cycleDays) {
        double riskScore = 0;

        // Overdue or approaching maintenance
        if (daysUntilNext < 0) {
            riskScore += 30;
        } else if (daysUntilNext < cycleDays * 0.2) {
            riskScore += 20;
        }

        // Failure history
        riskScore += Math.min(failureCount * 15, 40);

        // Equipment age
        if (ageDays != null && ageDays > 365 * 7) {
            riskScore += 20;
        } else if (ageDays != null && ageDays > 365 * 5) {
            riskScore += 15;
        }

        if (riskScore >= 60) return "CRITICAL";
        if (riskScore >= 40) return "HIGH";
        if (riskScore >= 20) return "MEDIUM";
        return "LOW";
    }

    private List<String> buildReasoning(LocalDate lastMaintenance, LocalDate predictedNext,
                                         Long daysSinceLast, Long daysUntilNext,
                                         int cycleDays, int failureCount, Long ageDays) {
        List<String> reasoning = new ArrayList<>();

        if (lastMaintenance != null) {
            reasoning.add(String.format("Last maintenance: %s (%d days ago)",
                    lastMaintenance, daysSinceLast));
        } else {
            reasoning.add("No maintenance history found");
        }

        reasoning.add(String.format("Predicted next maintenance: %s (%d days from now)",
                predictedNext, daysUntilNext));

        reasoning.add(String.format("Maintenance cycle: every %d days", cycleDays));

        if (failureCount > 0) {
            reasoning.add(String.format("Recent failures: %d emergency/corrective in last 180 days", failureCount));
        }

        if (ageDays != null) {
            reasoning.add(String.format("Equipment age: %d days (%.1f years)",
                    ageDays, ageDays / 365.0));
        }

        if (daysUntilNext < 0) {
            reasoning.add("WARNING: Maintenance is overdue by " + Math.abs(daysUntilNext) + " days");
        }

        return reasoning;
    }
}
