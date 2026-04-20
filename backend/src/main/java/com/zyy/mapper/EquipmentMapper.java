package com.zyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.model.entity.EquipmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_equipment persistence operations.
 *
 * @author System Architect
 * @see com.zyy.model.entity.EquipmentEntity
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<EquipmentEntity> {
}
