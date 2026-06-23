package com.zyy.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.inventory.model.entity.StockAlertEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_stock_alert persistence operations.
 *
 * @author System Architect
 * @see com.zyy.inventory.model.entity.StockAlertEntity
 */
@Mapper
public interface StockAlertMapper extends BaseMapper<StockAlertEntity> {
}
