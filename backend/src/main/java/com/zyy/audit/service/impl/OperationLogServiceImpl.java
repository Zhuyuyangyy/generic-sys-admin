package com.zyy.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.audit.mapper.OperationLogMapper;
import com.zyy.audit.model.entity.OperationLogEntity;
import com.zyy.audit.model.vo.OperationLogVO;
import com.zyy.audit.service.OperationLogService;
import com.zyy.common.PageVO;
import com.zyy.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of operation log business service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageVO<OperationLogVO> getPage(Long pageNum, Long pageSize, String module,
                                           String operator, LocalDateTime startTime, LocalDateTime endTime) {
        Page<OperationLogEntity> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<OperationLogEntity> queryWrapper = new LambdaQueryWrapper<>();

        if (module != null && !module.isBlank()) {
            queryWrapper.like(OperationLogEntity::getModule, module);
        }
        if (operator != null && !operator.isBlank()) {
            queryWrapper.like(OperationLogEntity::getOperatorName, operator);
        }
        if (startTime != null) {
            queryWrapper.ge(OperationLogEntity::getCreateTime, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(OperationLogEntity::getCreateTime, endTime);
        }

        queryWrapper.orderByDesc(OperationLogEntity::getCreateTime);

        Page<OperationLogEntity> result = operationLogMapper.selectPage(page, queryWrapper);

        List<OperationLogVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<OperationLogVO>builder()
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
    public OperationLogVO getById(Long id) {
        OperationLogEntity entity = operationLogMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OperationLogEntity entity = operationLogMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Operation log not found: " + id);
        }
        operationLogMapper.deleteById(id);
        log.info("Operation log deleted - logId={}", id);
    }

    private OperationLogVO entityToVO(OperationLogEntity entity) {
        if (entity == null) {
            return null;
        }

        String statusText = entity.getStatus() != null && entity.getStatus() == 1
                ? "Success" : "Failure";

        return OperationLogVO.builder()
                .id(entity.getId())
                .module(entity.getModule())
                .operation(entity.getOperation())
                .method(entity.getMethod())
                .requestParams(entity.getRequestParams())
                .responseResult(entity.getResponseResult())
                .operatorId(entity.getOperatorId())
                .operatorName(entity.getOperatorName())
                .ip(entity.getIp())
                .duration(entity.getDuration())
                .status(entity.getStatus())
                .statusText(statusText)
                .errorMessage(entity.getErrorMessage())
                .createTime(entity.getCreateTime())
                .build();
    }
}
