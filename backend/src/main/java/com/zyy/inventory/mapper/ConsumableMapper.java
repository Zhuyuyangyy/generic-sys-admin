package com.zyy.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.inventory.model.entity.ConsumableEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_consumable persistence operations.
 *
 * @author System Architect
 */
@Mapper
public interface ConsumableMapper extends BaseMapper<ConsumableEntity> {
}
