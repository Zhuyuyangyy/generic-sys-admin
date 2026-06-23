package com.zyy.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.inventory.model.entity.SupplierEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_supplier persistence operations.
 *
 * @author System Architect
 * @see com.zyy.inventory.model.entity.SupplierEntity
 */
@Mapper
public interface SupplierMapper extends BaseMapper<SupplierEntity> {
}
