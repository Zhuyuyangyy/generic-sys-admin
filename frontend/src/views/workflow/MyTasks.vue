<template>
  <div class="my-tasks-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的待办任务</span>
          <el-button text size="small" @click="fetchTasks">刷新</el-button>
        </div>
      </template>

      <el-empty v-if="tasks.length === 0 && !loading" description="暂无待办任务" />
      <div v-else class="task-list">
        <el-card v-for="task in tasks" :key="task.id" shadow="hover" class="task-card">
          <div class="task-header">
            <span class="task-title">{{ task.instanceTitle || `流程实例 #${task.instanceId}` }}</span>
            <el-tag size="small" type="warning">{{ task.statusText }}</el-tag>
          </div>
          <div class="task-body">
            <p><strong>步骤：</strong>{{ task.stepName }}</p>
            <p><strong>指派人：</strong>{{ task.assigneeName || '-' }}</p>
            <p><strong>创建时间：</strong>{{ task.createTime?.replace('T', ' ').slice(0, 19) || '-' }}</p>
          </div>
          <div class="task-actions">
            <el-input
              v-model="taskComments[task.id]"
              placeholder="审批意见"
              style="margin-bottom: 8px;"
            />
            <div class="action-buttons">
              <el-button type="success" :loading="actionLoading[task.id]" @click="handleApprove(task)">通过</el-button>
              <el-button type="danger" :loading="actionLoading[task.id]" @click="handleReject(task)">驳回</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyTasks, approveTask, rejectTask } from '@/api/workflow'
import type { WorkflowTaskVO } from '@/utils/types'

const loading = ref(false)
const tasks = ref<WorkflowTaskVO[]>([])
const taskComments = reactive<Record<number, string>>({})
const actionLoading = reactive<Record<number, boolean>>({})

const fetchTasks = async () => {
  loading.value = true
  try {
    tasks.value = await getMyTasks()
    tasks.value.forEach(t => { if (!taskComments[t.id]) taskComments[t.id] = '' })
  } catch {} finally { loading.value = false }
}

const handleApprove = async (task: WorkflowTaskVO) => {
  const comment = taskComments[task.id] || ''
  actionLoading[task.id] = true
  try {
    await approveTask(task.id, comment)
    ElMessage.success('审批通过')
    tasks.value = tasks.value.filter(t => t.id !== task.id)
  } catch {} finally { actionLoading[task.id] = false }
}

const handleReject = async (task: WorkflowTaskVO) => {
  const comment = taskComments[task.id]
  if (!comment) { ElMessage.warning('驳回时请填写意见'); return }
  try {
    await ElMessageBox.confirm('确认驳回此任务？', '提示', { type: 'warning' })
    actionLoading[task.id] = true
    await rejectTask(task.id, comment)
    ElMessage.success('已驳回')
    tasks.value = tasks.value.filter(t => t.id !== task.id)
  } catch {} finally { actionLoading[task.id] = false }
}

onMounted(() => { fetchTasks() })
</script>

<style scoped lang="scss">
.my-tasks-page { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.task-list { display: flex; flex-direction: column; gap: 12px; }
.task-card {
  .task-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; .task-title { font-weight: 600; font-size: 15px; } }
  .task-body { p { margin: 4px 0; font-size: 13px; color: #666; } }
  .task-actions { margin-top: 8px; .action-buttons { display: flex; gap: 8px; } }
}
</style>
