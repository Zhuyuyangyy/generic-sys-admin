package com.zyy.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.inventory.model.entity.BatchEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for consumable_batch persistence operations.
 *
 * @author System Architect
 * @see com.zyy.inventory.model.entity.BatchEntity
 */
@Mapper
public interface BatchMapper extends BaseMapper<BatchEntity> {
}
