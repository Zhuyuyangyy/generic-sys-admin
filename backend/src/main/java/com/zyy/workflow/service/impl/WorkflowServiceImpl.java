package com.zyy.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.common.PageVO;
import com.zyy.exception.BusinessException;
import com.zyy.workflow.mapper.WorkflowDefinitionMapper;
import com.zyy.workflow.mapper.WorkflowInstanceMapper;
import com.zyy.workflow.mapper.WorkflowTaskMapper;
import com.zyy.workflow.model.dto.WorkflowDefinitionSaveDTO;
import com.zyy.workflow.model.dto.WorkflowDefinitionUpdateDTO;
import com.zyy.workflow.model.dto.WorkflowInstanceStartDTO;
import com.zyy.workflow.model.dto.WorkflowTaskActionDTO;
import com.zyy.workflow.model.entity.WorkflowDefinitionEntity;
import com.zyy.workflow.model.entity.WorkflowInstanceEntity;
import com.zyy.workflow.model.entity.WorkflowTaskEntity;
import com.zyy.workflow.model.vo.WorkflowDefinitionVO;
import com.zyy.workflow.model.vo.WorkflowInstanceVO;
import com.zyy.workflow.model.vo.WorkflowTaskVO;
import com.zyy.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final WorkflowTaskMapper taskMapper;

    @Override
    public PageVO<WorkflowDefinitionVO> listDefinitions(Long pageNum, Long pageSize, String workflowType, Integer status) {
        Page<WorkflowDefinitionEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WorkflowDefinitionEntity> query = new LambdaQueryWrapper<>();
        query.eq(WorkflowDefinitionEntity::getIsDeleted, 0);

        if (workflowType != null && !workflowType.isBlank()) {
            query.eq(WorkflowDefinitionEntity::getWorkflowType, workflowType);
        }
        if (status != null) {
            query.eq(WorkflowDefinitionEntity::getStatus, status);
        }

        query.orderByDesc(WorkflowDefinitionEntity::getCreateTime);
        Page<WorkflowDefinitionEntity> result = definitionMapper.selectPage(page, query);

        List<WorkflowDefinitionVO> voList = result.getRecords().stream()
                .map(this::definitionEntityToVO)
                .collect(Collectors.toList());

        return PageVO.<WorkflowDefinitionVO>builder()
                .items(voList)
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .isFirst(result.getCurrent() == 1)
                .isLast(result.getCurrent() >= result.getPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionVO createDefinition(WorkflowDefinitionSaveDTO saveDTO, Long operatorId) {
        long existing = definitionMapper.selectCount(
                new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                        .eq(WorkflowDefinitionEntity::getDefinitionCode, saveDTO.getDefinitionCode())
                        .eq(WorkflowDefinitionEntity::getIsDeleted, 0)
        );
        if (existing > 0) {
            throw new BusinessException("Definition code already exists: " + saveDTO.getDefinitionCode());
        }

        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setDefinitionName(saveDTO.getDefinitionName());
        entity.setDefinitionCode(saveDTO.getDefinitionCode());
        entity.setDescription(saveDTO.getDescription());
        entity.setWorkflowType(saveDTO.getWorkflowType());
        entity.setSteps(saveDTO.getSteps());
        entity.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 1);
        entity.setVersion(saveDTO.getVersion() != null ? saveDTO.getVersion() : 1);
        entity.setCreateUser(operatorId);
        definitionMapper.insert(entity);

        log.info("Workflow definition created - id={}, code={}, operatorId={}",
                entity.getId(), entity.getDefinitionCode(), operatorId);

        return definitionEntityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionVO updateDefinition(WorkflowDefinitionUpdateDTO updateDTO, Long operatorId) {
        WorkflowDefinitionEntity entity = definitionMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Workflow definition not found: " + updateDTO.getId());
        }

        if (updateDTO.getDefinitionName() != null) entity.setDefinitionName(updateDTO.getDefinitionName());
        if (updateDTO.getDescription() != null) entity.setDescription(updateDTO.getDescription());
        if (updateDTO.getWorkflowType() != null) entity.setWorkflowType(updateDTO.getWorkflowType());
        if (updateDTO.getSteps() != null) entity.setSteps(updateDTO.getSteps());
        if (updateDTO.getStatus() != null) entity.setStatus(updateDTO.getStatus());
        if (updateDTO.getVersion() != null) entity.setVersion(updateDTO.getVersion());

        definitionMapper.updateById(entity);
        log.info("Workflow definition updated - id={}, operatorId={}", entity.getId(), operatorId);

        return definitionEntityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstanceVO startInstance(WorkflowInstanceStartDTO startDTO, Long initiatorId) {
        WorkflowDefinitionEntity definition = definitionMapper.selectById(startDTO.getDefinitionId());
        if (definition == null) {
            throw new BusinessException("Workflow definition not found: " + startDTO.getDefinitionId());
        }
        if (definition.getStatus() != 1) {
            throw new BusinessException("Workflow definition is not enabled: " + definition.getDefinitionCode());
        }

        int totalSteps = parseStepCount(definition.getSteps());

        WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
        instance.setDefinitionId(definition.getId());
        instance.setBusinessType(startDTO.getBusinessType());
        instance.setBusinessId(startDTO.getBusinessId());
        instance.setTitle(startDTO.getTitle());
        instance.setInitiatorId(initiatorId);
        instance.setCurrentStep(1);
        instance.setTotalSteps(totalSteps);
        instance.setStatus("PENDING");
        instance.setCreateUser(initiatorId);
        instanceMapper.insert(instance);

        WorkflowTaskEntity firstTask = new WorkflowTaskEntity();
        firstTask.setInstanceId(instance.getId());
        firstTask.setStepOrder(1);
        firstTask.setAssigneeId(initiatorId);
        firstTask.setAction("NONE");
        firstTask.setStatus("PENDING");
        firstTask.setCreateUser(initiatorId);
        taskMapper.insert(firstTask);

        log.info("Workflow instance started - id={}, definitionId={}, initiatorId={}",
                instance.getId(), definition.getId(), initiatorId);

        return instanceEntityToVO(instance, Collections.singletonList(firstTask));
    }

    @Override
    public PageVO<WorkflowInstanceVO> listInstances(Long pageNum, Long pageSize, String status, Long initiatorId) {
        Page<WorkflowInstanceEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WorkflowInstanceEntity> query = new LambdaQueryWrapper<>();
        query.eq(WorkflowInstanceEntity::getIsDeleted, 0);

        if (status != null && !status.isBlank()) {
            query.eq(WorkflowInstanceEntity::getStatus, status);
        }
        if (initiatorId != null) {
            query.eq(WorkflowInstanceEntity::getInitiatorId, initiatorId);
        }

        query.orderByDesc(WorkflowInstanceEntity::getCreateTime);
        Page<WorkflowInstanceEntity> result = instanceMapper.selectPage(page, query);

        List<WorkflowInstanceVO> voList = result.getRecords().stream()
                .map(inst -> {
                    List<WorkflowTaskEntity> tasks = taskMapper.selectList(
                            new LambdaQueryWrapper<WorkflowTaskEntity>()
                                    .eq(WorkflowTaskEntity::getInstanceId, inst.getId())
                                    .eq(WorkflowTaskEntity::getIsDeleted, 0)
                                    .orderByAsc(WorkflowTaskEntity::getStepOrder)
                    );
                    return instanceEntityToVO(inst, tasks);
                })
                .collect(Collectors.toList());

        return PageVO.<WorkflowInstanceVO>builder()
                .items(voList)
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .isFirst(result.getCurrent() == 1)
                .isLast(result.getCurrent() >= result.getPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
    }

    @Override
    public WorkflowInstanceVO getInstanceDetail(Long id) {
        WorkflowInstanceEntity instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException("Workflow instance not found: " + id);
        }

        List<WorkflowTaskEntity> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<WorkflowTaskEntity>()
                        .eq(WorkflowTaskEntity::getInstanceId, id)
                        .eq(WorkflowTaskEntity::getIsDeleted, 0)
                        .orderByAsc(WorkflowTaskEntity::getStepOrder)
        );

        return instanceEntityToVO(instance, tasks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskVO approveTask(Long taskId, WorkflowTaskActionDTO actionDTO, Long operatorId) {
        WorkflowTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Workflow task not found: " + taskId);
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("Task is not in PENDING status: " + taskId);
        }

        task.setAction("APPROVE");
        task.setComment(actionDTO != null ? actionDTO.getComment() : null);
        task.setStatus("APPROVED");
        task.setActionTime(LocalDateTime.now());
        taskMapper.updateById(task);

        WorkflowInstanceEntity instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw new BusinessException("Workflow instance not found for task: " + taskId);
        }

        if (task.getStepOrder() < instance.getTotalSteps()) {
            instance.setCurrentStep(task.getStepOrder() + 1);
            instanceMapper.updateById(instance);

            WorkflowTaskEntity nextTask = new WorkflowTaskEntity();
            nextTask.setInstanceId(instance.getId());
            nextTask.setStepOrder(task.getStepOrder() + 1);
            nextTask.setAssigneeId(operatorId);
            nextTask.setAction("NONE");
            nextTask.setStatus("PENDING");
            nextTask.setCreateUser(operatorId);
            taskMapper.insert(nextTask);

            log.info("Task approved, next task created - taskId={}, nextStep={}", taskId, nextTask.getStepOrder());
        } else {
            instance.setStatus("APPROVED");
            instanceMapper.updateById(instance);
            log.info("Workflow instance fully approved - instanceId={}", instance.getId());
        }

        return taskEntityToVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskVO rejectTask(Long taskId, WorkflowTaskActionDTO actionDTO, Long operatorId) {
        WorkflowTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Workflow task not found: " + taskId);
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("Task is not in PENDING status: " + taskId);
        }

        task.setAction("REJECT");
        task.setComment(actionDTO != null ? actionDTO.getComment() : null);
        task.setStatus("REJECTED");
        task.setActionTime(LocalDateTime.now());
        taskMapper.updateById(task);

        WorkflowInstanceEntity instance = instanceMapper.selectById(task.getInstanceId());
        if (instance != null) {
            instance.setStatus("REJECTED");
            instanceMapper.updateById(instance);
            log.info("Workflow instance rejected - instanceId={}", instance.getId());
        }

        return taskEntityToVO(task);
    }

    @Override
    public List<WorkflowTaskVO> getMyPendingTasks(Long assigneeId) {
        List<WorkflowTaskEntity> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<WorkflowTaskEntity>()
                        .eq(WorkflowTaskEntity::getAssigneeId, assigneeId)
                        .eq(WorkflowTaskEntity::getStatus, "PENDING")
                        .eq(WorkflowTaskEntity::getIsDeleted, 0)
                        .orderByAsc(WorkflowTaskEntity::getStepOrder)
        );

        return tasks.stream()
                .map(this::taskEntityToVO)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private int parseStepCount(String steps) {
        if (steps == null || steps.isBlank()) {
            return 1;
        }
        try {
            cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(steps);
            return arr.size();
        } catch (Exception e) {
            log.warn("Failed to parse steps JSON, defaulting to 1: {}", e.getMessage());
            return 1;
        }
    }

    private WorkflowDefinitionVO definitionEntityToVO(WorkflowDefinitionEntity entity) {
        if (entity == null) return null;
        return WorkflowDefinitionVO.builder()
                .id(entity.getId())
                .definitionName(entity.getDefinitionName())
                .definitionCode(entity.getDefinitionCode())
                .description(entity.getDescription())
                .workflowType(entity.getWorkflowType())
                .steps(entity.getSteps())
                .status(entity.getStatus())
                .statusText(entity.getStatus() == 1 ? "Enabled" : "Disabled")
                .version(entity.getVersion())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private WorkflowInstanceVO instanceEntityToVO(WorkflowInstanceEntity instance, List<WorkflowTaskEntity> tasks) {
        if (instance == null) return null;

        String definitionName = null;
        WorkflowDefinitionEntity definition = definitionMapper.selectById(instance.getDefinitionId());
        if (definition != null) {
            definitionName = definition.getDefinitionName();
        }

        List<WorkflowTaskVO> taskVOs = tasks != null
                ? tasks.stream().map(this::taskEntityToVO).collect(Collectors.toList())
                : Collections.emptyList();

        return WorkflowInstanceVO.builder()
                .id(instance.getId())
                .definitionId(instance.getDefinitionId())
                .definitionName(definitionName)
                .businessType(instance.getBusinessType())
                .businessId(instance.getBusinessId())
                .title(instance.getTitle())
                .initiatorId(instance.getInitiatorId())
                .currentStep(instance.getCurrentStep())
                .totalSteps(instance.getTotalSteps())
                .status(instance.getStatus())
                .createTime(instance.getCreateTime())
                .updateTime(instance.getUpdateTime())
                .tasks(taskVOs)
                .build();
    }

    private WorkflowTaskVO taskEntityToVO(WorkflowTaskEntity task) {
        if (task == null) return null;
        return WorkflowTaskVO.builder()
                .id(task.getId())
                .instanceId(task.getInstanceId())
                .stepOrder(task.getStepOrder())
                .assigneeId(task.getAssigneeId())
                .action(task.getAction())
                .comment(task.getComment())
                .status(task.getStatus())
                .actionTime(task.getActionTime())
                .createTime(task.getCreateTime())
                .build();
    }
}
