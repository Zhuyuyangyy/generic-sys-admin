package com.zyy.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.asset.model.entity.MaintenancePlanEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_maintenance_plan persistence operations.
 *
 * @author System Architect
 * @see com.zyy.asset.model.entity.MaintenancePlanEntity
 */
@Mapper
public interface MaintenancePlanMapper extends BaseMapper<MaintenancePlanEntity> {
}
