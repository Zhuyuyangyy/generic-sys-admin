<template>
  <div class="maintenance-page">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="设备">
          <el-input v-model="searchForm.equipmentId" placeholder="设备ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 130px">
            <el-option label="待激活" value="PENDING" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="已逾期" value="OVERDUE" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable style="width: 120px">
            <el-option label="日常保养" value="ROUTINE" />
            <el-option label="预防性维护" value="PREVENTIVE" />
            <el-option label="纠正性维修" value="CORRECTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <el-radio-group v-model="activeTab" @change="fetchData">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="overdue">逾期</el-radio-button>
              <el-radio-button value="upcoming">即将到期</el-radio-button>
            </el-radio-group>
          </div>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增计划</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="title" label="计划名称" min-width="140" />
        <el-table-column prop="equipmentName" label="设备名称" min-width="120" />
        <el-table-column prop="typeText" label="计划类型" width="110" />
        <el-table-column prop="scheduledDate" label="计划日期" width="110">
          <template #default="{ row }">{{ row.scheduledDate?.slice(0, 10) }}</template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="planStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priorityText" label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'MEDIUM' ? 'warning' : 'info'" size="small">
              {{ row.priorityText || row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" width="90" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleStatusUpdate(row, cmd)" v-if="row.status !== 'COMPLETED' && row.status !== 'CANCELLED'">
              <el-button size="small" type="warning">状态流转<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="ACTIVE" :disabled="row.status === 'ACTIVE'">激活</el-dropdown-item>
                  <el-dropdown-item command="COMPLETED" :disabled="row.status === 'COMPLETED'">完成</el-dropdown-item>
                  <el-dropdown-item command="CANCELLED" :disabled="row.status === 'CANCELLED'">取消</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="计划名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="设备ID" prop="equipmentId">
          <el-input-number v-model="form.equipmentId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="日常保养" value="ROUTINE" />
            <el-option label="预防性维护" value="PREVENTIVE" />
            <el-option label="纠正性维修" value="CORRECTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划日期" prop="scheduledDate">
          <el-date-picker v-model="form.scheduledDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input-number v-model="form.assigneeId" :min="1" style="width: 100%" placeholder="可选" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, ArrowDown } from '@element-plus/icons-vue'
import {
  getMaintenancePlans, createMaintenancePlan, updateMaintenancePlan, updatePlanStatus,
  deleteMaintenancePlan, getOverdueMaintenance, getUpcomingMaintenance,
  type MaintenancePlanSaveDTO,
} from '@/api/maintenance'
import type { MaintenancePlanVO } from '@/utils/types'

const planStatusType = (s: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    PENDING: 'info', ACTIVE: 'warning', COMPLETED: 'success', CANCELLED: 'info', OVERDUE: 'danger',
  }
  return map[s] || 'info'
}

const activeTab = ref('all')
const searchForm = reactive({ equipmentId: '', status: '', type: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const loading = ref(false)
const tableData = ref<MaintenancePlanVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

const defaultForm: MaintenancePlanSaveDTO = { equipmentId: 0, type: 'ROUTINE', title: '', description: '', scheduledDate: '', assigneeId: undefined, priority: 'MEDIUM' }
const form = reactive<MaintenancePlanSaveDTO>({ ...defaultForm })
const formRules = {
  title: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  equipmentId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
  type: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  scheduledDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
}

const fetchData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'overdue') {
      const res: any = await getOverdueMaintenance()
      tableData.value = Array.isArray(res) ? res : res?.data?.list || res?.data || []
      pagination.total = tableData.value.length
    } else if (activeTab.value === 'upcoming') {
      const res: any = await getUpcomingMaintenance(7)
      tableData.value = Array.isArray(res) ? res : res?.data?.list || res?.data || []
      pagination.total = tableData.value.length
    } else {
      const res: any = await getMaintenancePlans({
        pageNum: pagination.pageNum, pageSize: pagination.pageSize,
        equipmentId: searchForm.equipmentId || undefined,
        status: searchForm.status || undefined,
        type: searchForm.type || undefined,
      })
      const data = res?.data || res
      tableData.value = data?.list || []
      pagination.total = data?.total || 0
    }
  } catch {} finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { searchForm.equipmentId = ''; searchForm.status = ''; searchForm.type = ''; pagination.pageNum = 1; fetchData() }

const handleAdd = () => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, defaultForm)
  dialogTitle.value = '新增维保计划'; dialogVisible.value = true
}

const handleEdit = (row: MaintenancePlanVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, {
    title: row.title, equipmentId: row.equipmentId, type: row.type,
    description: row.description || '', scheduledDate: row.scheduledDate?.slice(0, 10) || '',
    assigneeId: row.assigneeId, priority: row.priority || 'MEDIUM',
  })
  dialogTitle.value = '编辑维保计划'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) { await updateMaintenancePlan(editingId.value, form); ElMessage.success('更新成功') }
    else { await createMaintenancePlan(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleStatusUpdate = async (row: MaintenancePlanVO, status: string) => {
  const labels: Record<string, string> = { ACTIVE: '激活', COMPLETED: '完成', CANCELLED: '取消' }
  try {
    await ElMessageBox.confirm(`确认将此计划标记为「${labels[status]}」？`, '提示', { type: 'warning' })
    await updatePlanStatus(row.id, status); ElMessage.success('状态更新成功'); fetchData()
  } catch {}
}

const handleDelete = async (row: MaintenancePlanVO) => {
  try {
    await ElMessageBox.confirm(`确认删除计划「${row.title}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await deleteMaintenancePlan(row.id); ElMessage.success('删除成功'); fetchData()
  } catch {}
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.maintenance-page { display: flex; flex-direction: column; gap: 12px; padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
