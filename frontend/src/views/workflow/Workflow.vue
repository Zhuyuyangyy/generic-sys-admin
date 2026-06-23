<template>
  <div class="workflow-page">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Definitions Tab -->
      <el-tab-pane label="流程定义" name="definitions">
        <div class="tab-header">
          <el-button type="primary" @click="handleAddDefinition"><el-icon><Plus /></el-icon> 新增定义</el-button>
        </div>
        <el-table :data="definitions" v-loading="defLoading" stripe border>
          <el-table-column prop="name" label="流程名称" min-width="140" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="description" label="描述" min-width="160" />
          <el-table-column prop="statusText" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="步骤数" width="80">
            <template #default="{ row }">{{ row.steps?.length || 0 }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 19) || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="handleEditDefinition(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Instances Tab -->
      <el-tab-pane label="流程实例" name="instances">
        <div class="tab-header">
          <el-select v-model="instanceStatus" placeholder="状态筛选" clearable style="width: 140px" @change="fetchInstances">
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </div>
        <el-table :data="instances" v-loading="instLoading" stripe border>
          <el-table-column prop="title" label="流程标题" min-width="160" />
          <el-table-column prop="definitionName" label="流程定义" width="140" />
          <el-table-column prop="initiatorName" label="发起人" width="100" />
          <el-table-column prop="statusText" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="instanceStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发起时间" width="160">
            <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 19) || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="viewInstance(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Definition dialog -->
    <el-dialog v-model="defDialogVisible" :title="defDialogTitle" width="680px" destroy-on-close>
      <el-form :model="defForm" :rules="defFormRules" ref="defFormRef" label-width="90px">
        <el-form-item label="流程名称" prop="name">
          <el-input v-model="defForm.name" placeholder="请输入流程名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="defForm.type" style="width: 100%">
            <el-option label="设备审批" value="EQUIPMENT_APPROVAL" />
            <el-option label="采购审批" value="PURCHASE_APPROVAL" />
            <el-option label="维保审批" value="MAINTENANCE_APPROVAL" />
            <el-option label="通用审批" value="GENERAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="defForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="审批步骤">
          <div v-for="(step, idx) in defForm.steps" :key="idx" class="step-row">
            <el-input v-model="step.name" placeholder="步骤名称" style="width: 160px" />
            <el-input v-model="step.approverRole" placeholder="审批角色" style="width: 140px" />
            <el-input-number v-model="step.order" :min="1" style="width: 100px" />
            <el-button type="danger" link @click="defForm.steps.splice(idx, 1)">删除</el-button>
          </div>
          <el-button type="primary" link @click="defForm.steps.push({ name: '', approverRole: '', order: defForm.steps.length + 1 })">+ 添加步骤</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="defDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="defSaveLoading" @click="handleSaveDefinition">确认</el-button>
      </template>
    </el-dialog>

    <!-- Instance detail dialog -->
    <el-dialog v-model="instDetailVisible" title="流程实例详情" width="600px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentInstance">
        <el-descriptions-item label="流程标题">{{ currentInstance.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="instanceStatusType(currentInstance.status)" size="small">{{ currentInstance.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发起人">{{ currentInstance.initiatorName }}</el-descriptions-item>
        <el-descriptions-item label="发起时间">{{ currentInstance.createTime?.replace('T', ' ').slice(0, 19) }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>任务时间线</el-divider>
      <el-timeline v-if="instanceTasks.length > 0">
        <el-timeline-item v-for="task in instanceTasks" :key="task.id" :timestamp="task.createTime?.replace('T', ' ').slice(0, 19)">
          <div>
            <strong>{{ task.stepName }}</strong> - {{ task.assigneeName }}
            <el-tag size="small" :type="task.status === 'COMPLETED' ? 'success' : task.status === 'REJECTED' ? 'danger' : 'warning'" style="margin-left: 8px;">
              {{ task.statusText }}
            </el-tag>
          </div>
          <div v-if="task.comment" style="color: #666; font-size: 13px; margin-top: 4px;">{{ task.comment }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无任务记录" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getWorkflowDefinitions, createWorkflowDefinition, updateWorkflowDefinition,
  getWorkflowInstances, getWorkflowInstance,
  type WorkflowDefinitionSaveDTO, type WorkflowStepDTO,
} from '@/api/workflow'
import type { WorkflowDefinitionVO, WorkflowInstanceVO, WorkflowTaskVO } from '@/utils/types'
import request from '@/utils/request'

const instanceStatusType = (s: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    ACTIVE: 'warning', COMPLETED: 'success', REJECTED: 'danger', CANCELLED: 'info',
  }
  return map[s] || 'info'
}

const activeTab = ref('definitions')

// Definitions
const defLoading = ref(false)
const definitions = ref<WorkflowDefinitionVO[]>([])
const defDialogVisible = ref(false)
const defDialogTitle = ref('')
const defSaveLoading = ref(false)
const defFormRef = ref()
const isEditDef = ref(false)
const editingDefId = ref<number>()

const defForm = reactive<WorkflowDefinitionSaveDTO>({ name: '', description: '', type: 'GENERAL', steps: [] })
const defFormRules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

// Instances
const instLoading = ref(false)
const instances = ref<WorkflowInstanceVO[]>([])
const instanceStatus = ref('')
const instDetailVisible = ref(false)
const currentInstance = ref<WorkflowInstanceVO | null>(null)
const instanceTasks = ref<WorkflowTaskVO[]>([])

const fetchDefinitions = async () => {
  defLoading.value = true
  try {
    const res: any = await getWorkflowDefinitions({ pageNum: 1, pageSize: 100 })
    const data = res?.data || res
    definitions.value = data?.list || (Array.isArray(data) ? data : [])
  } catch {} finally { defLoading.value = false }
}

const fetchInstances = async () => {
  instLoading.value = true
  try {
    const res: any = await getWorkflowInstances({ pageNum: 1, pageSize: 100, status: instanceStatus.value || undefined })
    const data = res?.data || res
    instances.value = data?.list || (Array.isArray(data) ? data : [])
  } catch {} finally { instLoading.value = false }
}

const handleAddDefinition = () => {
  isEditDef.value = false; editingDefId.value = undefined
  Object.assign(defForm, { name: '', description: '', type: 'GENERAL', steps: [] })
  defDialogTitle.value = '新增流程定义'; defDialogVisible.value = true
}

const handleEditDefinition = (row: WorkflowDefinitionVO) => {
  isEditDef.value = true; editingDefId.value = row.id
  Object.assign(defForm, {
    name: row.name, description: row.description || '', type: row.type,
    steps: (row.steps || []).map(s => ({ name: s.name, approverRole: s.approverRole || '', approverId: s.approverId, order: s.order })),
  })
  defDialogTitle.value = '编辑流程定义'; defDialogVisible.value = true
}

const handleSaveDefinition = async () => {
  const valid = await defFormRef.value?.validate().catch(() => false)
  if (!valid) return
  defSaveLoading.value = true
  try {
    if (isEditDef.value && editingDefId.value) { await updateWorkflowDefinition(editingDefId.value, defForm); ElMessage.success('更新成功') }
    else { await createWorkflowDefinition(defForm); ElMessage.success('新增成功') }
    defDialogVisible.value = false; fetchDefinitions()
  } catch {} finally { defSaveLoading.value = false }
}

const viewInstance = async (row: WorkflowInstanceVO) => {
  currentInstance.value = row
  try {
    const detail = await getWorkflowInstance(row.id)
    currentInstance.value = detail
    // Fetch tasks for this instance
    const res: any = await request.get(`/workflow/instances/${row.id}/tasks`)
    instanceTasks.value = Array.isArray(res) ? res : res?.data || []
  } catch {}
  instDetailVisible.value = true
}

onMounted(() => { fetchDefinitions(); fetchInstances() })
</script>

<style scoped lang="scss">
.workflow-page { padding: 10px; }
.tab-header { margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }
.step-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
</style>
