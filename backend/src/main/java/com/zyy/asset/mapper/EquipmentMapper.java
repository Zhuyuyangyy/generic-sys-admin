package com.zyy.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.asset.model.entity.EquipmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_equipment persistence operations.
 *
 * @author System Architect
 * @see com.zyy.asset.model.entity.EquipmentEntity
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<EquipmentEntity> {
}
