<template>
  <div class="inspection-page">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="设备ID">
          <el-input v-model="searchForm.equipmentId" placeholder="设备ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 130px">
            <el-option label="待巡检" value="PENDING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="巡检类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable style="width: 120px">
            <el-option label="日常巡检" value="DAILY" />
            <el-option label="周巡检" value="WEEKLY" />
            <el-option label="月度巡检" value="MONTHLY" />
            <el-option label="专项巡检" value="SPECIAL" />
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
          <span>巡检记录</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新建巡检</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="equipmentName" label="设备名称" min-width="140" />
        <el-table-column prop="typeText" label="巡检类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ row.typeText || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inspectorName" label="巡检人" width="100" />
        <el-table-column prop="scheduledDate" label="计划日期" width="110">
          <template #default="{ row }">{{ row.scheduledDate?.slice(0, 10) }}</template>
        </el-table-column>
        <el-table-column prop="completedDate" label="完成日期" width="110">
          <template #default="{ row }">{{ row.completedDate?.slice(0, 10) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="inspectionStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="findings" label="巡检发现" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
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
        <el-form-item label="设备ID" prop="equipmentId">
          <el-input-number v-model="form.equipmentId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="巡检类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="日常巡检" value="DAILY" />
            <el-option label="周巡检" value="WEEKLY" />
            <el-option label="月度巡检" value="MONTHLY" />
            <el-option label="专项巡检" value="SPECIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划日期" prop="scheduledDate">
          <el-date-picker v-model="form.scheduledDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="巡检人ID">
          <el-input-number v-model="form.inspectorId" :min="1" style="width: 100%" placeholder="可选" />
        </el-form-item>
        <el-form-item label="巡检发现">
          <el-input v-model="form.findings" type="textarea" :rows="3" placeholder="请输入巡检发现" />
        </el-form-item>
        <el-form-item label="建议措施">
          <el-input v-model="form.recommendations" type="textarea" :rows="2" placeholder="请输入建议措施" />
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
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import type { InspectionVO, InspectionSaveDTO } from '@/utils/types'

const inspectionStatusType = (s: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', CANCELLED: 'info',
  }
  return map[s] || 'info'
}

const searchForm = reactive({ equipmentId: '', status: '', type: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const loading = ref(false)
const tableData = ref<InspectionVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

const defaultForm: InspectionSaveDTO = {
  equipmentId: 0, type: 'DAILY', scheduledDate: '',
  inspectorId: undefined, findings: '', recommendations: '',
}
const form = reactive<InspectionSaveDTO>({ ...defaultForm })
const formRules = {
  equipmentId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
  type: [{ required: true, message: '请选择巡检类型', trigger: 'change' }],
  scheduledDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/equipment/inspections', {
      params: {
        pageNum: pagination.pageNum, pageSize: pagination.pageSize,
        equipmentId: searchForm.equipmentId || undefined,
        status: searchForm.status || undefined,
        type: searchForm.type || undefined,
      },
    })
    const data = res?.data || res
    tableData.value = data?.list || (Array.isArray(data) ? data : [])
    pagination.total = data?.total || 0
  } catch {} finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => {
  searchForm.equipmentId = ''; searchForm.status = ''; searchForm.type = ''
  pagination.pageNum = 1; fetchData()
}

const handleAdd = () => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, defaultForm)
  dialogTitle.value = '新建巡检'; dialogVisible.value = true
}

const handleEdit = (row: InspectionVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, {
    equipmentId: row.equipmentId, type: row.type,
    scheduledDate: row.scheduledDate?.slice(0, 10) || '',
    inspectorId: row.inspectorId,
    findings: row.findings || '', recommendations: row.recommendations || '',
  })
  dialogTitle.value = '编辑巡检'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) {
      await request.put(`/equipment/inspections/${editingId.value}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/equipment/inspections', form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.inspection-page { display: flex; flex-direction: column; gap: 12px; padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
