package com.zyy.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.workflow.model.entity.WorkflowTaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowTaskMapper extends BaseMapper<WorkflowTaskEntity> {
}
