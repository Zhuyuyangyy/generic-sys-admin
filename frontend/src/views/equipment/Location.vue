<template>
  <div class="location-page">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>位置树</span>
              <el-button type="primary" size="small" @click="handleAdd()"><el-icon><Plus /></el-icon> 新增位置</el-button>
            </div>
          </template>
          <el-tree
            :data="locationTree"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            v-loading="loading"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <span>{{ data.name }}</span>
                <span class="node-actions">
                  <el-button size="small" type="primary" link @click.stop="handleAdd(data)">新增</el-button>
                  <el-button size="small" type="danger" link @click.stop="handleDelete(data)">删除</el-button>
                </span>
              </div>
            </template>
          </el-tree>
          <el-empty v-if="locationTree.length === 0 && !loading" description="暂无位置数据" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ isEdit ? '编辑位置' : '位置详情' }}</span>
              <div v-if="selectedLocation">
                <el-button type="primary" size="small" @click="handleEdit(selectedLocation!)">编辑</el-button>
              </div>
            </div>
          </template>
          <el-descriptions v-if="selectedLocation && !isEdit" :column="1" border>
            <el-descriptions-item label="名称">{{ selectedLocation.name }}</el-descriptions-item>
            <el-descriptions-item label="楼栋">{{ selectedLocation.building || '-' }}</el-descriptions-item>
            <el-descriptions-item label="楼层">{{ selectedLocation.floor || '-' }}</el-descriptions-item>
            <el-descriptions-item label="房间">{{ selectedLocation.room || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ selectedLocation.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备数量">{{ selectedLocation.equipmentCount ?? 0 }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-if="!selectedLocation && !isEdit" description="请选择一个位置" />

          <el-form v-if="isEdit" :model="form" :rules="formRules" ref="formRef" label-width="80px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入位置名称" />
            </el-form-item>
            <el-form-item label="楼栋">
              <el-input v-model="form.building" placeholder="请输入楼栋" />
            </el-form-item>
            <el-form-item label="楼层">
              <el-input v-model="form.floor" placeholder="请输入楼层" />
            </el-form-item>
            <el-form-item label="房间">
              <el-input v-model="form.room" placeholder="请输入房间号" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
              <el-button @click="isEdit = false">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="addDialogVisible" title="新增位置" width="480px" destroy-on-close>
      <el-form :model="addForm" :rules="formRules" ref="addFormRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入位置名称" />
        </el-form-item>
        <el-form-item label="楼栋">
          <el-input v-model="addForm.building" placeholder="请输入楼栋" />
        </el-form-item>
        <el-form-item label="楼层">
          <el-input v-model="addForm.floor" placeholder="请输入楼层" />
        </el-form-item>
        <el-form-item label="房间">
          <el-input v-model="addForm.room" placeholder="请输入房间号" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="addForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleAddSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getLocations, createLocation, updateLocation, deleteLocation, type LocationSaveDTO } from '@/api/location'
import type { LocationVO } from '@/utils/types'

const loading = ref(false)
const locationTree = ref<LocationVO[]>([])
const selectedLocation = ref<LocationVO | null>(null)

const isEdit = ref(false)
const saveLoading = ref(false)
const formRef = ref()
const form = reactive<LocationSaveDTO & { id?: number }>({ name: '', building: '', floor: '', room: '', description: '' })
const formRules = { name: [{ required: true, message: '请输入位置名称', trigger: 'blur' }] }

const addDialogVisible = ref(false)
const addFormRef = ref()
const addForm = reactive<LocationSaveDTO>({ name: '', building: '', floor: '', room: '', description: '' })

const fetchData = async () => {
  loading.value = true
  try {
    locationTree.value = await getLocations()
  } catch {} finally { loading.value = false }
}

const handleNodeClick = (data: LocationVO) => {
  selectedLocation.value = data
  isEdit.value = false
}

const handleAdd = (parent?: LocationVO) => {
  Object.assign(addForm, { name: '', building: parent?.building || '', floor: parent?.floor || '', room: '', description: '' })
  addDialogVisible.value = true
}

const handleAddSave = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    await createLocation(addForm)
    ElMessage.success('新增成功'); addDialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleEdit = (data: LocationVO) => {
  isEdit.value = true
  Object.assign(form, { id: data.id, name: data.name, building: data.building || '', floor: data.floor || '', room: data.room || '', description: data.description || '' })
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    await updateLocation(form.id!, { name: form.name, building: form.building, floor: form.floor, room: form.room, description: form.description })
    ElMessage.success('更新成功'); isEdit.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleDelete = async (data: LocationVO) => {
  try {
    await ElMessageBox.confirm(`确认删除位置「${data.name}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await deleteLocation(data.id)
    ElMessage.success('删除成功')
    if (selectedLocation.value?.id === data.id) selectedLocation.value = null
    fetchData()
  } catch {}
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.location-page { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tree-node {
  display: flex; align-items: center; justify-content: space-between; width: 100%;
  .node-actions { display: none; }
  &:hover .node-actions { display: inline-flex; gap: 4px; }
}
</style>
