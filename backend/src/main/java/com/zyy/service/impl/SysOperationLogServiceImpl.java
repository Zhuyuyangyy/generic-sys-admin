package com.zyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.mapper.SysOperationLogMapper;
import com.zyy.model.entity.SysOperationLogEntity;
import com.zyy.model.vo.PageVO;
import com.zyy.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl implements SysOperationLogService {

    private final SysOperationLogMapper logMapper;

    @Async("logAsyncExecutor")
    @Override
    public void saveLog(SysOperationLogEntity entity) {
        try {
            logMapper.insert(entity);
            log.debug("操作日志已保存 - module={}, operation={}, user={}",
                    entity.getModule(), entity.getOperation(), entity.getUsername());
        } catch (Exception e) {
            log.error("操作日志保存失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public PageVO<SysOperationLogEntity> getPage(Long pageNum, Long pageSize,
            String module, String operation, String operator, String status,
            String startTime, String endTime) {
        LambdaQueryWrapper<SysOperationLogEntity> wrapper = new LambdaQueryWrapper<>();

        if (hasText(module)) {
            wrapper.like(SysOperationLogEntity::getModule, module);
        }
        if (hasText(operation)) {
            wrapper.eq(SysOperationLogEntity::getOperation, operation);
        }
        if (hasText(operator)) {
            wrapper.like(SysOperationLogEntity::getUsername, operator);
        }
        if (hasText(status)) {
            wrapper.eq(SysOperationLogEntity::getResultStatus, Integer.valueOf(status));
        }
        if (hasText(startTime)) {
            wrapper.ge(SysOperationLogEntity::getOperationTime, LocalDateTime.parse(startTime + " 00:00:00"));
        }
        if (hasText(endTime)) {
            wrapper.le(SysOperationLogEntity::getOperationTime, LocalDateTime.parse(endTime + " 23:59:59"));
        }

        wrapper.orderByDesc(SysOperationLogEntity::getOperationTime);

        Page<SysOperationLogEntity> page = new Page<>(pageNum, pageSize);
        logMapper.selectPage(page, wrapper);

        return PageVO.<SysOperationLogEntity>builder()
                .items(page.getRecords())
                .total(page.getTotal())
                .pages(page.getPages())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .isFirst(page.getCurrent() == 1)
                .isLast(page.getCurrent() >= page.getPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}