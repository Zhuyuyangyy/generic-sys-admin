package com.zyy.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.asset.model.entity.LocationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for sys_location persistence operations.
 *
 * @author System Architect
 * @see com.zyy.asset.model.entity.LocationEntity
 */
@Mapper
public interface LocationMapper extends BaseMapper<LocationEntity> {
}
