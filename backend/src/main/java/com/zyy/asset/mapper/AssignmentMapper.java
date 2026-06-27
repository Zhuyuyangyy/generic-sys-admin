package com.zyy.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.asset.model.entity.AssignmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for asset_assignment persistence operations.
 *
 * @author System Architect
 * @see com.zyy.asset.model.entity.AssignmentEntity
 */
@Mapper
public interface AssignmentMapper extends BaseMapper<AssignmentEntity> {
}
