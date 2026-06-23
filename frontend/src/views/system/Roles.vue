<template>
  <div class="roles-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增角色</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="code" label="角色编码" width="140" />
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="statusText" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleAssignMenus(row)">分配菜单</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>

    <!-- Menu assignment dialog -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="480px" destroy-on-close>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuSaveLoading" @click="handleSaveMenus">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getRoles, createRole, updateRole, deleteRole, assignMenus, getRoleMenus } from '@/api/role'
import type { RoleSaveDTO } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import type { RoleVO, MenuVO } from '@/utils/types'

const loading = ref(false)
const tableData = ref<RoleVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

const form = reactive<RoleSaveDTO>({ name: '', code: '', description: '', status: 1 })
const formRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

const menuDialogVisible = ref(false)
const menuSaveLoading = ref(false)
const menuTree = ref<MenuVO[]>([])
const menuTreeRef = ref()
const checkedMenuIds = ref<number[]>([])
const currentRoleId = ref<number>()

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getRoles({ pageNum: 1, pageSize: 100 })
    const data = res?.data || res
    tableData.value = data?.list || (Array.isArray(data) ? data : [])
  } catch {} finally { loading.value = false }
}

const fetchMenuTree = async () => {
  try { menuTree.value = await getMenuTree() } catch {}
}

const handleAdd = () => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, { name: '', code: '', description: '', status: 1 })
  dialogTitle.value = '新增角色'; dialogVisible.value = true
}

const handleEdit = (row: RoleVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, { name: row.name, code: row.code, description: row.description || '', status: row.status })
  dialogTitle.value = '编辑角色'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) { await updateRole(editingId.value, form); ElMessage.success('更新成功') }
    else { await createRole(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleDelete = async (row: RoleVO) => {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await deleteRole(row.id); ElMessage.success('删除成功'); fetchData()
  } catch {}
}

const handleAssignMenus = async (row: RoleVO) => {
  currentRoleId.value = row.id
  await fetchMenuTree()
  try {
    const res: any = await getRoleMenus(row.id)
    const ids = Array.isArray(res) ? res : res?.data || []
    checkedMenuIds.value = ids.map((m: any) => typeof m === 'number' ? m : m.id)
  } catch { checkedMenuIds.value = [] }
  menuDialogVisible.value = true
}

const handleSaveMenus = async () => {
  if (!currentRoleId.value) return
  const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) || []
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
  const allKeys = [...checkedKeys, ...halfCheckedKeys]
  menuSaveLoading.value = true
  try {
    await assignMenus(currentRoleId.value, allKeys)
    ElMessage.success('菜单分配成功'); menuDialogVisible.value = false
  } catch {} finally { menuSaveLoading.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.roles-page { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
