package com.zyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.mapper.EquipmentMapper;
import com.zyy.mapper.SysOperationLogMapper;
import com.zyy.mapper.SysUserMapper;
import com.zyy.model.entity.EquipmentEntity;
import com.zyy.model.entity.ConsumableEntity;
import com.zyy.mapper.ConsumableMapper;
import com.zyy.model.entity.SysOperationLogEntity;
import com.zyy.model.entity.SysUserEntity;
import com.zyy.model.vo.DashboardStatsVO;
import com.zyy.model.vo.DashboardStatsVO.*;
import com.zyy.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard 统计聚合 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EquipmentMapper equipmentMapper;
    private final ConsumableMapper consumableMapper;
    private final SysUserMapper userMapper;
    private final SysOperationLogMapper operationLogMapper;

    @Override
    public DashboardStatsVO getStats() {
        return DashboardStatsVO.builder()
                .equipment(buildEquipmentStats())
                .consumable(buildConsumableStats())
                .user(buildUserStats())
                .logTrend(buildLogTrend())
                .equipmentStatus(buildEquipmentStatus())
                .operationType(buildOperationType())
                .build();
    }

    private EquipmentStats buildEquipmentStats() {
        long total = equipmentMapper.selectCount(null);
        long normal = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getStatus, 1));
        long maintenance = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getStatus, 0));
        long scrapped = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getStatus, 2));
        return EquipmentStats.builder()
                .total(total).normal(normal).maintenance(maintenance).scrapped(scrapped)
                .build();
    }

    private ConsumableStats buildConsumableStats() {
        long total = consumableMapper.selectCount(null);
        long lowStock = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>().lt(ConsumableEntity::getStockQuantity, 10));
        LocalDate thirtyDaysLater = LocalDate.now().plusDays(30);
        long expiring = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .le(ConsumableEntity::getExpirationDate, thirtyDaysLater)
                        .isNotNull(ConsumableEntity::getExpirationDate));
        return ConsumableStats.builder()
                .total(total).lowStock(lowStock).expiring(expiring)
                .build();
    }

    private UserStats buildUserStats() {
        long total = userMapper.selectCount(null);
        long active = userMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getStatus, 1));
        long disabled = userMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getStatus, 0));
        return UserStats.builder().total(total).active(active).disabled(disabled).build();
    }

    private List<LogTrend> buildLogTrend() {
        List<LogTrend> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime now = LocalDateTime.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.minusDays(i).toLocalDate();
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = operationLogMapper.selectCount(
                    new LambdaQueryWrapper<SysOperationLogEntity>()
                            .ge(SysOperationLogEntity::getOperationTime, start)
                            .lt(SysOperationLogEntity::getOperationTime, end));
            trend.add(LogTrend.builder()
                    .date(date.format(fmt))
                    .count(count)
                    .build());
        }
        return trend;
    }

    private List<StatusCount> buildEquipmentStatus() {
        List<StatusCount> list = new ArrayList<>();
        list.add(StatusCount.builder().label("正常").value(Long.valueOf(equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getStatus, 1))).intValue()).build());
        list.add(StatusCount.builder().label("维护中").value(Long.valueOf(equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getStatus, 0))).intValue()).build());
        list.add(StatusCount.builder().label("已报废").value(Long.valueOf(equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>().eq(EquipmentEntity::getStatus, 2))).intValue()).build());
        return list;
    }

    private List<StatusCount> buildOperationType() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        List<SysOperationLogEntity> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<SysOperationLogEntity>()
                        .ge(SysOperationLogEntity::getOperationTime, start));

        Map<String, Long> counts = logs.stream()
                .collect(Collectors.groupingBy(SysOperationLogEntity::getOperation, Collectors.counting()));


        return counts.entrySet().stream()
                .map(e -> StatusCount.builder().label(e.getKey()).value(e.getValue().intValue()).build())
                .collect(Collectors.toList());
    }
}