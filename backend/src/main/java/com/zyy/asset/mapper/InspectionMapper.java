package com.zyy.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.asset.model.entity.InspectionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for asset_inspection persistence operations.
 *
 * @author System Architect
 * @see com.zyy.asset.model.entity.InspectionEntity
 */
@Mapper
public interface InspectionMapper extends BaseMapper<InspectionEntity> {
}
