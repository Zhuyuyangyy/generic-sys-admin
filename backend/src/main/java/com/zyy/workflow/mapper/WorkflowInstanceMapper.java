package com.zyy.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.workflow.model.entity.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceEntity> {
}
