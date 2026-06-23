package com.zyy.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zyy.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_workflow_definition")
public class WorkflowDefinitionEntity extends BaseEntity {

    private String definitionName;

    private String definitionCode;

    private String description;

    private String workflowType;

    private String steps;

    private Integer status;

    private Integer version;
}
