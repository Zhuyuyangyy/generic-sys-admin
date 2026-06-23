package com.zyy.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.audit.model.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper interface for operation_log persistence operations.
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}
