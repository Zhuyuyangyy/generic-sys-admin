package com.zyy.workflow.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.workflow.model.dto.WorkflowDefinitionSaveDTO;
import com.zyy.workflow.model.dto.WorkflowDefinitionUpdateDTO;
import com.zyy.workflow.model.dto.WorkflowInstanceStartDTO;
import com.zyy.workflow.model.dto.WorkflowTaskActionDTO;
import com.zyy.workflow.model.vo.WorkflowDefinitionVO;
import com.zyy.workflow.model.vo.WorkflowInstanceVO;
import com.zyy.workflow.model.vo.WorkflowTaskVO;
import com.zyy.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Tag(name = "Workflow Management", description = "Lightweight approval workflow for business processes")
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/definitions")
    @Operation(summary = "List workflow definitions", description = "Paginated list with optional filters by workflowType and status")
    public Result<PageVO<WorkflowDefinitionVO>> listDefinitions(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String workflowType,
            @RequestParam(required = false) Integer status) {
        return Result.ok(workflowService.listDefinitions(
                pageParam.getPageNum(), pageParam.getPageSize(), workflowType, status));
    }

    @PostMapping("/definitions")
    @Operation(summary = "Create workflow definition", description = "Define a new approval workflow with steps configuration")
    public Result<WorkflowDefinitionVO> createDefinition(
            @Valid @RequestBody WorkflowDefinitionSaveDTO saveDTO,
            HttpServletRequest request) {
        WorkflowDefinitionVO vo = workflowService.createDefinition(saveDTO, getCurrentUserId(request));
        return Result.ok(vo, "Workflow definition created successfully");
    }

    @PutMapping("/definitions/{id}")
    @Operation(summary = "Update workflow definition")
    public Result<WorkflowDefinitionVO> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowDefinitionUpdateDTO updateDTO,
            HttpServletRequest request) {
        updateDTO.setId(id);
        WorkflowDefinitionVO vo = workflowService.updateDefinition(updateDTO, getCurrentUserId(request));
        return Result.ok(vo, "Workflow definition updated successfully");
    }

    @PostMapping("/instances")
    @Operation(summary = "Start workflow instance", description = "Initiate a workflow for a business object")
    public Result<WorkflowInstanceVO> startInstance(
            @Valid @RequestBody WorkflowInstanceStartDTO startDTO,
            HttpServletRequest request) {
        WorkflowInstanceVO vo = workflowService.startInstance(startDTO, getCurrentUserId(request));
        return Result.ok(vo, "Workflow instance started successfully");
    }

    @GetMapping("/instances")
    @Operation(summary = "List workflow instances", description = "Paginated list with optional filters by status and initiatorId")
    public Result<PageVO<WorkflowInstanceVO>> listInstances(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long initiatorId) {
        return Result.ok(workflowService.listInstances(
                pageParam.getPageNum(), pageParam.getPageSize(), status, initiatorId));
    }

    @GetMapping("/instances/{id}")
    @Operation(summary = "Get workflow instance detail", description = "Returns instance with all associated tasks")
    public Result<WorkflowInstanceVO> getInstanceDetail(@PathVariable Long id) {
        return Result.ok(workflowService.getInstanceDetail(id));
    }

    @PostMapping("/tasks/{id}/approve")
    @Operation(summary = "Approve a workflow task", description = "Approve the current pending task and advance the workflow")
    public Result<WorkflowTaskVO> approveTask(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowTaskActionDTO actionDTO,
            HttpServletRequest request) {
        WorkflowTaskVO vo = workflowService.approveTask(id, actionDTO, getCurrentUserId(request));
        return Result.ok(vo, "Task approved successfully");
    }

    @PostMapping("/tasks/{id}/reject")
    @Operation(summary = "Reject a workflow task", description = "Reject the current pending task and terminate the workflow")
    public Result<WorkflowTaskVO> rejectTask(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowTaskActionDTO actionDTO,
            HttpServletRequest request) {
        WorkflowTaskVO vo = workflowService.rejectTask(id, actionDTO, getCurrentUserId(request));
        return Result.ok(vo, "Task rejected");
    }

    @GetMapping("/my-tasks")
    @Operation(summary = "Get my pending tasks", description = "Returns pending tasks assigned to the current user")
    public Result<List<WorkflowTaskVO>> getMyPendingTasks(HttpServletRequest request) {
        return Result.ok(workflowService.getMyPendingTasks(getCurrentUserId(request)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
