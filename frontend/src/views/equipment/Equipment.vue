<template>
  <div class="equipment-page">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.name" placeholder="设备名称/编码" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="searchForm.category" placeholder="请选择类别" clearable style="width: 150px">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="维修中" :value="0" />
            <el-option label="报废" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>设备列表</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增设备</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="serialNumber" label="设备编码" min-width="140" />
        <el-table-column prop="name" label="设备名称" min-width="140" />
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="model" label="型号" min-width="120" />
        <el-table-column prop="statusText" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status]" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="存放地点" min-width="120" />
        <el-table-column prop="nextMaintenanceDate" label="下次维保" width="110">
          <template #default="{ row }">{{ row.nextMaintenanceDate?.slice(0, 10) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleStatusCommand(row, cmd)">
              <el-button size="small" type="warning">状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="1" :disabled="row.status === 1">恢复正常</el-dropdown-item>
                  <el-dropdown-item command="0" :disabled="row.status === 0">标记维修</el-dropdown-item>
                  <el-dropdown-item command="2" :disabled="row.status === 2">标记报废</el-dropdown-item>
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
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" placeholder="请输入型号" />
        </el-form-item>
        <el-form-item label="序列号" prop="serialNumber">
          <el-input v-model="form.serialNumber" placeholder="请输入序列号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" :value="1" />
            <el-option label="维修中" :value="0" />
            <el-option label="报废" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="存放地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入存放地点" />
        </el-form-item>
        <el-form-item label="采购日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="采购价格">
          <el-input-number v-model="form.purchasePrice" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.supplier" placeholder="请输入供应商" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="请输入备注" />
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
  getEquipmentPage, saveEquipment, updateEquipment, updateEquipmentStatus, deleteEquipment,
  type EquipmentVO, type EquipmentSaveDTO, type EquipmentStatus,
} from '@/api/equipment'

const statusTypeMap: Record<number, 'warning' | 'success' | 'danger'> = { 0: 'warning', 1: 'success', 2: 'danger' }
const categoryOptions = ['办公设备', '生产设备', '检测设备', '运输设备', '其他']

const searchForm = reactive({ name: '', category: '', status: undefined as EquipmentStatus | undefined })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const loading = ref(false)
const tableData = ref<EquipmentVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

const defaultForm: EquipmentSaveDTO = {
  name: '', category: '', model: '', serialNumber: '', status: 1,
  location: '', purchaseDate: '', purchasePrice: undefined, supplier: '', remarks: '',
}
const form = reactive<EquipmentSaveDTO>({ ...defaultForm })

const formRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  serialNumber: [{ required: true, message: '请输入序列号', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  location: [{ required: true, message: '请输入存放地点', trigger: 'blur' }],
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getEquipmentPage({
      pageNum: pagination.pageNum, pageSize: pagination.pageSize,
      name: searchForm.name || undefined, category: searchForm.category || undefined, status: searchForm.status,
    })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } catch {} finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { searchForm.name = ''; searchForm.category = ''; searchForm.status = undefined; pagination.pageNum = 1; fetchData() }

const handleAdd = () => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, defaultForm)
  dialogTitle.value = '新增设备'; dialogVisible.value = true
}

const handleEdit = (row: EquipmentVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, {
    name: row.name, category: row.category, model: row.model, serialNumber: row.serialNumber,
    status: row.status, location: row.location,
    purchaseDate: (row as any).purchaseDate || '', purchasePrice: (row as any).purchasePrice ?? undefined,
    supplier: (row as any).supplier || '', remarks: (row as any).remarks || '',
  })
  dialogTitle.value = '编辑设备'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) { await updateEquipment(editingId.value, form); ElMessage.success('更新成功') }
    else { await saveEquipment(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleStatusCommand = async (row: EquipmentVO, cmd: string) => {
  const status = Number(cmd) as EquipmentStatus
  const labels: Record<number, string> = { 0: '维修中', 1: '正常', 2: '报废' }
  try {
    await ElMessageBox.confirm(`确认将设备状态更改为「${labels[status]}」？`, '提示', { type: 'warning' })
    await updateEquipmentStatus(row.id, status); ElMessage.success('状态更新成功'); fetchData()
  } catch {}
}

const handleDelete = async (row: EquipmentVO) => {
  try {
    await ElMessageBox.confirm(`确认删除设备「${row.name}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await deleteEquipment(row.id); ElMessage.success('删除成功'); fetchData()
  } catch {}
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.equipment-page { display: flex; flex-direction: column; gap: 12px; padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
