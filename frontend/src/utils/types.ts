/**
 * 通用响应包装器
 * @description 统一前后端数据响应结构，所有 API 响应均包装于此
 */
export interface Result<T = any> {
  /** 业务状态码，200 表示成功 */
  code: number
  /** 响应描述信息 */
  message: string
  /** 响应数据负载 */
  data: T
  /** 响应时间戳 */
  timestamp?: number
  /** 请求链路追踪 ID */
  traceId?: string
}

/**
 * 分页请求参数
 */
export interface PageParam {
  pageNum: number
  pageSize: number
}

/**
 * 分页响应数据
 */
export interface PageVO<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// ==================== 角色管理 ====================

/**
 * 角色视图对象
 */
export interface RoleVO {
  id: number
  name: string
  code: string
  description?: string
  status: number
  statusText: string
  createTime: string
  updateTime?: string
}

// ==================== 菜单管理 ====================

/**
 * 菜单视图对象
 */
export interface MenuVO {
  id: number
  parentId?: number
  name: string
  path?: string
  component?: string
  icon?: string
  sort?: number
  visible?: boolean
  type?: number
  permission?: string
  children?: MenuVO[]
  createTime: string
  updateTime?: string
}

// ==================== 审计日志 ====================

/**
 * 操作日志视图对象
 */
export interface OperationLogVO {
  id: number
  userId: number
  username: string
  action: string
  module: string
  method: string
  params?: string
  ip: string
  duration: number
  status: number
  statusText: string
  errorMsg?: string
  createTime: string
}

/**
 * 异常事件
 */
export interface AnomalyEvent {
  id: number
  type: string
  severity: string
  description: string
  userId?: number
  username?: string
  details?: Record<string, any>
  resolved: boolean
  resolvedAt?: string
  resolvedBy?: number
  createTime: string
}

/**
 * 异常事件汇总统计
 */
export interface AnomalySummary {
  total: number
  unresolved: number
  byType: Record<string, number>
  bySeverity: Record<string, number>
  recentCount: number
}

// ==================== 工作流 ====================

/**
 * 工作流步骤视图对象
 */
export interface WorkflowStepVO {
  id: number
  name: string
  approverRole?: string
  approverId?: number
  order: number
  status?: string
}

/**
 * 工作流定义视图对象
 */
export interface WorkflowDefinitionVO {
  id: number
  name: string
  description?: string
  type: string
  steps: WorkflowStepVO[]
  status: number
  statusText: string
  createTime: string
  updateTime?: string
}

/**
 * 工作流实例视图对象
 */
export interface WorkflowInstanceVO {
  id: number
  definitionId: number
  definitionName?: string
  title: string
  status: string
  statusText: string
  currentStep?: number
  variables?: Record<string, any>
  initiatorId: number
  initiatorName?: string
  createTime: string
  updateTime?: string
  completeTime?: string
}

/**
 * 工作流任务视图对象
 */
export interface WorkflowTaskVO {
  id: number
  instanceId: number
  instanceTitle?: string
  stepName: string
  assigneeId: number
  assigneeName?: string
  status: string
  statusText: string
  comment?: string
  createTime: string
  updateTime?: string
}

// ==================== 维保计划 ====================

/**
 * 维保计划视图对象
 */
export interface MaintenancePlanVO {
  id: number
  equipmentId: number
  equipmentName?: string
  type: string
  typeText?: string
  title: string
  description?: string
  scheduledDate: string
  completedDate?: string
  assigneeId?: number
  assigneeName?: string
  priority?: string
  priorityText?: string
  status: string
  statusText: string
  createTime: string
  updateTime?: string
}

// ==================== 位置管理 ====================

/**
 * 位置视图对象
 */
export interface LocationVO {
  id: number
  name: string
  building?: string
  floor?: string
  room?: string
  description?: string
  equipmentCount?: number
  createTime: string
  updateTime?: string
}

// ==================== 供应商管理 ====================

/**
 * 供应商视图对象
 */
export interface SupplierVO {
  id: number
  name: string
  contactPerson?: string
  phone?: string
  email?: string
  address?: string
  description?: string
  consumableCount?: number
  createTime: string
  updateTime?: string
}

// ==================== 库存预警 ====================

/**
 * 库存预警视图对象
 */
export interface StockAlertVO {
  id: number
  consumableId: number
  consumableName?: string
  type: string
  severity: string
  message: string
  currentStock: number
  threshold: number
  acknowledged: boolean
  acknowledgedBy?: number
  acknowledgedAt?: string
  createTime: string
}

// ==================== 仪表盘 ====================

/**
 * 仪表盘概览数据
 */
export interface DashboardOverview {
  totalEquipment: number
  normalEquipment: number
  maintenanceEquipment: number
  scrappedEquipment: number
  totalConsumables: number
  lowStockConsumables: number
  pendingMaintenancePlans: number
  overdueMaintenancePlans: number
  activeWorkflowInstances: number
  pendingTasks: number
}

/**
 * 设备状态分布
 */
export interface EquipmentStatusDistribution {
  normal: number
  maintenance: number
  scrapped: number
}

/**
 * 库存汇总数据
 */
export interface InventorySummary {
  totalCategories: number
  totalItems: number
  totalValue: number
  lowStockItems: number
  outOfStockItems: number
  expiringItems: number
}

/**
 * 最近活动
 */
export interface RecentActivity {
  id: number
  type: string
  action: string
  description: string
  username?: string
  timestamp: string
}

// ==================== 设备健康度 ====================

/**
 * 设备健康评分
 */
export interface AssetHealthScore {
  equipmentId: number
  equipmentName: string
  score: number
  level: string
  factors: HealthFactor[]
  lastCheckedAt: string
}

/**
 * 健康影响因素
 */
export interface HealthFactor {
  name: string
  score: number
  weight: number
  description?: string
}

/**
 * 设备健康概览
 */
export interface AssetHealthOverview {
  averageScore: number
  healthyCount: number
  warningCount: number
  criticalCount: number
  distribution: HealthDistribution[]
}

/**
 * 健康度分布
 */
export interface HealthDistribution {
  level: string
  count: number
  percentage: number
}

// ==================== 自然语言交互 ====================

/**
 * 试运行结果
 */
export interface DryRunResult {
  confirmationId: string
  input: string
  parsedIntent: string
  affectedEntities: AffectedEntity[]
  warnings: string[]
  estimatedImpact: string
}

/**
 * 受影响实体
 */
export interface AffectedEntity {
  type: string
  id: number
  name: string
  change: string
}

/**
 * 执行结果
 */
export interface ExecuteResult {
  success: boolean
  message: string
  confirmationId?: string
  results?: ExecutionDetail[]
  causalWarnings?: CausalWarning[]
}

/**
 * 执行详情
 */
export interface ExecutionDetail {
  action: string
  target: string
  status: string
  detail?: string
}

/**
 * 因果警告
 */
export interface CausalWarning {
  type: string
  description: string
  severity: string
  relatedEntities?: string[]
}

// ==================== 报表 ====================

/**
 * 资产汇总报表
 */
export interface AssetSummaryReport {
  totalAssets: number
  totalValue: number
  byCategory: CategorySummary[]
  byStatus: StatusSummary[]
  byLocation: LocationSummary[]
  period: string
}

/**
 * 库存汇总报表
 */
export interface InventorySummaryReport {
  totalConsumables: number
  totalValue: number
  totalStock: number
  byCategory: CategorySummary[]
  lowStockItems: LowStockItem[]
  expiringItems: ExpiringItem[]
  period: string
}

/**
 * 维保历史报表
 */
export interface MaintenanceHistoryReport {
  totalPlans: number
  completedPlans: number
  overduePlans: number
  averageCompletionDays: number
  byType: TypeSummary[]
  byEquipment: EquipmentMaintenanceSummary[]
  period: string
}

/**
 * 审计轨迹报表
 */
export interface AuditTrailReport {
  totalLogs: number
  byModule: ModuleSummary[]
  byAction: ActionSummary[]
  byUser: UserActivitySummary[]
  anomalies: AnomalySummaryItem[]
  period: string
}

/**
 * 库存变动报表
 */
export interface StockMovementReport {
  totalInbound: number
  totalOutbound: number
  totalAdjustments: number
  netChange: number
  byConsumable: ConsumableMovement[]
  byDate: DateMovement[]
  period: string
}

// ==================== 报表辅助类型 ====================

/**
 * 分类汇总
 */
export interface CategorySummary {
  category: string
  count: number
  value: number
}

/**
 * 状态汇总
 */
export interface StatusSummary {
  status: string
  statusText: string
  count: number
  value: number
}

/**
 * 位置汇总
 */
export interface LocationSummary {
  location: string
  count: number
  value: number
}

/**
 * 低库存项
 */
export interface LowStockItem {
  id: number
  name: string
  category: string
  currentStock: number
  minStockLevel: number
  shortage: number
}

/**
 * 即将过期项
 */
export interface ExpiringItem {
  id: number
  name: string
  category: string
  expirationDate: string
  daysUntilExpiry: number
  currentStock: number
}

/**
 * 类型汇总
 */
export interface TypeSummary {
  type: string
  typeText: string
  count: number
}

/**
 * 设备维保汇总
 */
export interface EquipmentMaintenanceSummary {
  equipmentId: number
  equipmentName: string
  totalPlans: number
  completedPlans: number
  averageCompletionDays: number
}

/**
 * 模块汇总
 */
export interface ModuleSummary {
  module: string
  count: number
}

/**
 * 操作汇总
 */
export interface ActionSummary {
  action: string
  count: number
}

/**
 * 用户活动汇总
 */
export interface UserActivitySummary {
  userId: number
  username: string
  actionCount: number
}

/**
 * 异常摘要项
 */
export interface AnomalySummaryItem {
  id: number
  type: string
  severity: string
  description: string
  createTime: string
}

/**
 * 耗材变动
 */
export interface ConsumableMovement {
  consumableId: number
  consumableName: string
  inbound: number
  outbound: number
  adjustments: number
  netChange: number
}

/**
 * 日期变动
 */
export interface DateMovement {
  date: string
  inbound: number
  outbound: number
  adjustments: number
}
