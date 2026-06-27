package com.zyy.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.tenant.model.entity.TenantEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper interface for sys_tenant persistence operations.
 */
@Mapper
public interface TenantMapper extends BaseMapper<TenantEntity> {
}
