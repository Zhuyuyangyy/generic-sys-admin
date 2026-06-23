<template>
  <div class="category-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备类别管理</span>
          <el-button type="primary" size="small" @click="handleAdd()"><el-icon><Plus /></el-icon> 新增类别</el-button>
        </div>
      </template>
      <el-tree
        :data="treeData"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
        v-loading="loading"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span class="node-label">{{ data.name }}</span>
            <span class="node-actions">
              <el-button size="small" type="primary" link @click="handleAdd(data)">新增子级</el-button>
              <el-button size="small" type="primary" link @click="handleEdit(data)">编辑</el-button>
              <el-button size="small" type="danger" link @click="handleDelete(data)">删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
      <el-empty v-if="treeData.length === 0 && !loading" description="暂无类别数据" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="类别名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入类别名称" />
        </el-form-item>
        <el-form-item label="上级类别">
          <el-input :value="parentName" disabled />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface CategoryNode {
  id: number
  name: string
  parentId?: number
  description?: string
  children?: CategoryNode[]
}

const loading = ref(false)
const treeData = ref<CategoryNode[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()
const parentName = ref('无')

const form = reactive({ name: '', description: '', parentId: undefined as number | undefined })
const formRules = { name: [{ required: true, message: '请输入类别名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/equipment/categories')
    treeData.value = Array.isArray(res) ? res : res?.data || []
  } catch {} finally { loading.value = false }
}

const handleAdd = (parent?: CategoryNode) => {
  isEdit.value = false; editingId.value = undefined
  form.name = ''; form.description = ''
  form.parentId = parent?.id; parentName.value = parent?.name || '无'
  dialogTitle.value = '新增类别'; dialogVisible.value = true
}

const handleEdit = (data: CategoryNode) => {
  isEdit.value = true; editingId.value = data.id
  form.name = data.name; form.description = data.description || ''
  form.parentId = data.parentId; parentName.value = '编辑'
  dialogTitle.value = '编辑类别'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) {
      await request.put(`/equipment/categories/${editingId.value}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/equipment/categories', form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleDelete = async (data: CategoryNode) => {
  try {
    await ElMessageBox.confirm(`确认删除类别「${data.name}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await request.delete(`/equipment/categories/${data.id}`)
    ElMessage.success('删除成功'); fetchData()
  } catch {}
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.category-page { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tree-node {
  display: flex; align-items: center; justify-content: space-between; width: 100%;
  .node-label { font-size: 14px; }
  .node-actions { display: none; }
  &:hover .node-actions { display: inline-flex; gap: 4px; }
}
</style>
