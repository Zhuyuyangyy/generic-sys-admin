<template>
  <div class="menus-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button type="primary" @click="handleAdd()"><el-icon><Plus /></el-icon> 新增菜单</el-button>
        </div>
      </template>

      <el-table
        :data="menuTree"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        border
        default-expand-all
      >
        <el-table-column prop="name" label="菜单名称" min-width="180" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.type)" size="small">{{ menuTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160" />
        <el-table-column prop="component" label="组件" min-width="160" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column prop="permission" label="权限标识" min-width="140" />
        <el-table-column label="可见" width="70">
          <template #default="{ row }">
            <el-tag :type="row.visible !== false ? 'success' : 'info'" size="small">{{ row.visible !== false ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleAdd(row)">新增子级</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="上级菜单">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径" v-if="form.type !== 2">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="form.type === 1">
          <el-input v-model="form.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="图标" v-if="form.type !== 2">
          <el-input v-model="form.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="form.type === 2">
          <el-input v-model="form.permission" placeholder="如：user:create" />
        </el-form-item>
        <el-form-item label="是否可见">
          <el-switch v-model="form.visible" />
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
import { getMenuTree, createMenu, updateMenu, deleteMenu, type MenuSaveDTO } from '@/api/menu'
import type { MenuVO } from '@/utils/types'

const menuTypeLabel = (type?: number) => { const m: Record<number, string> = { 0: '目录', 1: '菜单', 2: '按钮' }; return m[type ?? 1] || '未知' }
const menuTypeTag = (type?: number): '' | 'success' | 'warning' | 'danger' | 'info' => { const m: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = { 0: '', 1: 'success', 2: 'warning' }; return m[type ?? 1] || 'info' }

const loading = ref(false)
const menuTree = ref<MenuVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()
const parentName = ref('无')

const defaultForm: MenuSaveDTO = { name: '', path: '', component: '', icon: '', sort: 0, visible: true, type: 1, permission: '' }
const form = reactive<MenuSaveDTO>({ ...defaultForm })
const formRules = { name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try { menuTree.value = await getMenuTree() } catch {} finally { loading.value = false }
}

const handleAdd = (parent?: MenuVO) => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, { ...defaultForm, parentId: parent?.id, type: parent ? (parent.type === 0 ? 1 : 2) : 0 })
  parentName.value = parent?.name || '无'
  dialogTitle.value = '新增菜单'; dialogVisible.value = true
}

const handleEdit = (row: MenuVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, {
    parentId: row.parentId, name: row.name, path: row.path || '', component: row.component || '',
    icon: row.icon || '', sort: row.sort || 0, visible: row.visible !== false,
    type: row.type ?? 1, permission: row.permission || '',
  })
  parentName.value = '编辑'
  dialogTitle.value = '编辑菜单'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) { await updateMenu(editingId.value, form); ElMessage.success('更新成功') }
    else { await createMenu(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleDelete = async (row: MenuVO) => {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.name}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await deleteMenu(row.id); ElMessage.success('删除成功'); fetchData()
  } catch {}
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.menus-page { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
