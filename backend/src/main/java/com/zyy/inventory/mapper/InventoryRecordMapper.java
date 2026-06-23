package com.zyy.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.inventory.model.entity.InventoryRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_inventory_record persistence operations.
 *
 * @author System Architect
 */
@Mapper
public interface InventoryRecordMapper extends BaseMapper<InventoryRecordEntity> {
}
