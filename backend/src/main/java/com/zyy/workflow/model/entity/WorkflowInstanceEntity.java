package com.zyy.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zyy.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_workflow_instance")
public class WorkflowInstanceEntity extends BaseEntity {

    private Long definitionId;

    private String businessType;

    private Long businessId;

    private String title;

    private Long initiatorId;

    private Integer currentStep;

    private Integer totalSteps;

    private String status;
}
