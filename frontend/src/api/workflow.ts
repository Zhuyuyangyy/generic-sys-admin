/**
 * 工作流模块 API
 * @description 工作流定义、实例、审批任务等接口封装
 */
import request from '@/utils/request'
import type { WorkflowDefinitionVO, WorkflowInstanceVO, WorkflowTaskVO } from '@/utils/types'

/**
 * 工作流定义新增/编辑请求参数
 */
export interface WorkflowDefinitionSaveDTO {
  name: string
  description?: string
  type: string
  steps: WorkflowStepDTO[]
}

/**
 * 工作流步骤定义
 */
export interface WorkflowStepDTO {
  name: string
  approverRole?: string
  approverId?: number
  order: number
}

/**
 * 发起工作流请求参数
 */
export interface StartWorkflowDTO {
  definitionId: number
  title: string
  variables?: Record<string, any>
}

/**
 * 工作流定义分页查询参数
 */
export interface WorkflowDefinitionPageParam {
  pageNum: number
  pageSize: number
  name?: string
  type?: string
}

/**
 * 工作流实例分页查询参数
 */
export interface WorkflowInstancePageParam {
  pageNum: number
  pageSize: number
  status?: string
  definitionId?: number
}

/**
 * 分页查询工作流定义列表
 * @param params - 分页参数及过滤条件
 */
export const getWorkflowDefinitions = (params: any) =>
  request.get('/workflow/definitions', { params })

/**
 * 新增工作流定义
 * @param data - 工作流定义信息
 */
export const createWorkflowDefinition = (data: WorkflowDefinitionSaveDTO): Promise<WorkflowDefinitionVO> =>
  request.post<WorkflowDefinitionVO>('/workflow/definitions', data) as unknown as Promise<WorkflowDefinitionVO>

/**
 * 更新工作流定义
 * @param id - 工作流定义唯一标识
 * @param data - 更新后的工作流定义信息
 */
export const updateWorkflowDefinition = (id: number, data: WorkflowDefinitionSaveDTO): Promise<WorkflowDefinitionVO> =>
  request.put<WorkflowDefinitionVO>(`/workflow/definitions/${id}`, data) as unknown as Promise<WorkflowDefinitionVO>

/**
 * 发起工作流实例
 * @param data - 工作流发起参数
 */
export const startWorkflow = (data: StartWorkflowDTO): Promise<WorkflowInstanceVO> =>
  request.post<WorkflowInstanceVO>('/workflow/instances', data) as unknown as Promise<WorkflowInstanceVO>

/**
 * 分页查询工作流实例列表
 * @param params - 分页参数及过滤条件
 */
export const getWorkflowInstances = (params: any) =>
  request.get('/workflow/instances', { params })

/**
 * 根据 ID 查询工作流实例详情
 * @param id - 工作流实例唯一标识
 */
export const getWorkflowInstance = (id: number): Promise<WorkflowInstanceVO> =>
  request.get<WorkflowInstanceVO>(`/workflow/instances/${id}`) as unknown as Promise<WorkflowInstanceVO>

/**
 * 审批通过任务
 * @param id - 任务唯一标识
 * @param comment - 审批意见
 */
export const approveTask = (id: number, comment: string): Promise<void> =>
  request.post<void>(`/workflow/tasks/${id}/approve`, { comment }) as unknown as Promise<void>

/**
 * 审批驳回任务
 * @param id - 任务唯一标识
 * @param comment - 驳回意见
 */
export const rejectTask = (id: number, comment: string): Promise<void> =>
  request.post<void>(`/workflow/tasks/${id}/reject`, { comment }) as unknown as Promise<void>

/**
 * 获取当前用户的待办任务
 */
export const getMyTasks = (): Promise<WorkflowTaskVO[]> =>
  request.get<WorkflowTaskVO[]>('/workflow/my-tasks') as unknown as Promise<WorkflowTaskVO[]>
