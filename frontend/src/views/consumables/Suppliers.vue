<template>
  <div class="suppliers-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>供应商管理</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增供应商</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="name" label="供应商编码" width="120">
          <template #default="{ row }">{{ `SUP-${String(row.id).padStart(4, '0')}` }}</template>
        </el-table-column>
        <el-table-column prop="name" label="供应商名称" min-width="140" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="供应商名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" />
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
import { Plus } from '@element-plus/icons-vue'
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier, type SupplierSaveDTO } from '@/api/supplier'
import type { SupplierVO } from '@/utils/types'

const loading = ref(false)
const tableData = ref<SupplierVO[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

const defaultForm: SupplierSaveDTO = { name: '', contactPerson: '', phone: '', email: '', address: '', description: '' }
const form = reactive<SupplierSaveDTO>({ ...defaultForm })
const formRules = { name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getSuppliers({ pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    const data = res?.data || res
    tableData.value = data?.list || (Array.isArray(data) ? data : [])
    pagination.total = data?.total || 0
  } catch {} finally { loading.value = false }
}

const handleAdd = () => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, defaultForm)
  dialogTitle.value = '新增供应商'; dialogVisible.value = true
}

const handleEdit = (row: SupplierVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, { name: row.name, contactPerson: row.contactPerson || '', phone: row.phone || '', email: row.email || '', address: row.address || '', description: row.description || '' })
  dialogTitle.value = '编辑供应商'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) { await updateSupplier(editingId.value, form); ElMessage.success('更新成功') }
    else { await createSupplier(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleDelete = async (row: SupplierVO) => {
  try {
    await ElMessageBox.confirm(`确认删除供应商「${row.name}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await deleteSupplier(row.id); ElMessage.success('删除成功'); fetchData()
  } catch {}
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.suppliers-page { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
