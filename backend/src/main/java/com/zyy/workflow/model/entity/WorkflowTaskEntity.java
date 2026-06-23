package com.zyy.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zyy.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_workflow_task")
public class WorkflowTaskEntity extends BaseEntity {

    private Long instanceId;

    private Integer stepOrder;

    private Long assigneeId;

    private String action;

    private String comment;

    private String status;

    private LocalDateTime actionTime;
}
