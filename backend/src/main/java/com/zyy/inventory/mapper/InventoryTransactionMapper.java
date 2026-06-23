package com.zyy.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.inventory.model.entity.InventoryTransactionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_inventory_transaction persistence operations.
 *
 * @author System Architect
 */
@Mapper
public interface InventoryTransactionMapper extends BaseMapper<InventoryTransactionEntity> {
}
