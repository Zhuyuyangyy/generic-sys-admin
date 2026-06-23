package com.zyy.workflow.service;

import com.zyy.common.PageVO;
import com.zyy.workflow.model.dto.WorkflowDefinitionSaveDTO;
import com.zyy.workflow.model.dto.WorkflowDefinitionUpdateDTO;
import com.zyy.workflow.model.dto.WorkflowInstanceStartDTO;
import com.zyy.workflow.model.dto.WorkflowTaskActionDTO;
import com.zyy.workflow.model.vo.WorkflowDefinitionVO;
import com.zyy.workflow.model.vo.WorkflowInstanceVO;
import com.zyy.workflow.model.vo.WorkflowTaskVO;

import java.util.List;

public interface WorkflowService {

    PageVO<WorkflowDefinitionVO> listDefinitions(Long pageNum, Long pageSize, String workflowType, Integer status);

    WorkflowDefinitionVO createDefinition(WorkflowDefinitionSaveDTO saveDTO, Long operatorId);

    WorkflowDefinitionVO updateDefinition(WorkflowDefinitionUpdateDTO updateDTO, Long operatorId);

    WorkflowInstanceVO startInstance(WorkflowInstanceStartDTO startDTO, Long initiatorId);

    PageVO<WorkflowInstanceVO> listInstances(Long pageNum, Long pageSize, String status, Long initiatorId);

    WorkflowInstanceVO getInstanceDetail(Long id);

    WorkflowTaskVO approveTask(Long taskId, WorkflowTaskActionDTO actionDTO, Long operatorId);

    WorkflowTaskVO rejectTask(Long taskId, WorkflowTaskActionDTO actionDTO, Long operatorId);

    List<WorkflowTaskVO> getMyPendingTasks(Long assigneeId);
}
