<template>
  <div class="equipment-page">
    <!-- 搜索栏：条件过滤 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="设备名称">
          <el-input v-model="searchForm.name" placeholder="请输入设备名称" clearable style="width: 180px" />
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
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域：数据展示与操作 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>设备列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增设备
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="设备名称" min-width="140" />
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="model" label="型号" min-width="120" />
        <el-table-column prop="serialNumber" label="序列号" min-width="140" />
        <el-table-column prop="statusText" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status]" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="存放地点" min-width="120" />
        <el-table-column prop="purchasePrice" label="采购价格" width="100">
          <template #default="{ row }">
            {{ row.purchasePrice != null ? `¥${row.purchasePrice}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ').slice(0, 19) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="info" @click="handleView3D(row)">
              <el-icon><View /></el-icon> 3D
            </el-button>
            <el-button size="small" type="warning" @click="handleStatus(row)">
              {{ row.status === 1 ? '维修' : row.status === 0 ? '恢复正常' : '报废' }}
            </el-button>
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

    <!-- 新增/编辑弹窗 -->
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

    <!-- 3D 设备详情弹窗 -->
    <el-dialog v-model="threeDVisible" :title="`设备 3D 预览 - ${threeDRow?.name || ''}`" width="860px" destroy-on-close>
      <div class="three-wrapper">
        <div ref="threeContainerRef" class="three-canvas" />
        <div class="three-sidebar">
          <h4>设备信息</h4>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="名称">{{ threeDRow?.name }}</el-descriptions-item>
            <el-descriptions-item label="型号">{{ threeDRow?.model }}</el-descriptions-item>
            <el-descriptions-item label="序列号">{{ threeDRow?.serialNumber }}</el-descriptions-item>
            <el-descriptions-item label="类别">{{ threeDRow?.category }}</el-descriptions-item>
            <el-descriptions-item label="存放地点">{{ threeDRow?.location }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTypeMap[threeDRow?.status ?? 1]" size="small">
                {{ threeDRow?.statusText }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div class="three-controls">
            <el-button size="small" @click="threeDAutoRotate = !threeDAutoRotate">
              {{ threeDAutoRotate ? '停止旋转' : '自动旋转' }}
            </el-button>
            <el-button size="small" @click="resetThreeCamera">重置视角</el-button>
          </div>
          <el-alert type="info" :closable="false" style="margin-top: 12px;">
            <template #title>
              <span style="font-size: 12px;">💡 拖拽旋转 · 滚轮缩放 · 右键平移</span><br />
              <span style="font-size: 12px;">📦 DentalRobot.glb 模型加载位已预留</span>
            </template>
          </el-alert>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 设备管理视图
 * @description 设备资产全生命周期管理页面，基于 CrudTemplate 模式实现搜索、分页、新增、编辑、状态流转及软删除
 */
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, View, InfoFilled } from '@element-plus/icons-vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import {
  getEquipmentPage,
  saveEquipment,
  updateEquipment,
  updateEquipmentStatus,
  deleteEquipment,
  type EquipmentVO,
  type EquipmentSaveDTO,
  type EquipmentStatus,
} from '@/api/equipment'

/** 状态码与 Tag 类型映射 */
const statusTypeMap: Record<number, 'warning' | 'success' | 'danger'> = { 0: 'warning', 1: 'success', 2: 'danger' }

const categoryOptions = ['办公设备', '生产设备', '检测设备', '运输设备', '其他']

/** 搜索表单 */
const searchForm = reactive({ name: '', category: '', status: undefined as EquipmentStatus | undefined })
/** 分页参数 */
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const loading = ref(false)
const tableData = ref<EquipmentVO[]>([])

/** 弹窗控制 */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

/** 表单默认空数据 */
const defaultForm: EquipmentSaveDTO = {
  name: '', category: '', model: '', serialNumber: '', status: 1,
  location: '', purchaseDate: '', purchasePrice: undefined, supplier: '', remarks: '',
}
const form = reactive<EquipmentSaveDTO>({ ...defaultForm })

/** 表单校验规则 */
const formRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  serialNumber: [{ required: true, message: '请输入序列号', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  location: [{ required: true, message: '请输入存放地点', trigger: 'blur' }],
}

/** ========== 3D 预览相关 ========== */
const threeDVisible = ref(false)
const threeDRow = ref<EquipmentVO | null>(null)
const threeContainerRef = ref()
let renderer: THREE.WebGLRenderer
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let controls: OrbitControls
let animationId: number
let cube: THREE.Mesh
let threeDAutoRotate = ref(false)

/**
 * 初始化 Three.js 渲染器
 * @description 构建场景、相机、OrbitControls 轨道控制器；预留 GLTF/GLB 模型加载接口
 */
const initThreeScene = (container: HTMLElement) => {
  // 场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf0f2f5)

  // 相机
  camera = new THREE.PerspectiveCamera(60, container.clientWidth / container.clientHeight, 0.1, 1000)
  camera.position.set(3, 2, 3)

  // 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(container.clientWidth, container.clientHeight)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  container.appendChild(renderer.domElement)

  // 轨道控制器（鼠标拖拽旋转/缩放/平移）
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.autoRotate = threeDAutoRotate.value
  controls.autoRotateSpeed = 2.0

  // 网格地面（参考平面）
  const grid = new THREE.GridHelper(6, 20, 0xdddddd, 0xe8e8e8)
  scene.add(grid)

  // 设备展示台
  const platformGeo = new THREE.CylinderGeometry(0.8, 1.0, 0.08, 32)
  const platformMat = new THREE.MeshStandardMaterial({ color: 0x409EFF, metalness: 0.3, roughness: 0.5 })
  const platform = new THREE.Mesh(platformGeo, platformMat)
  platform.position.y = -0.04
  platform.receiveShadow = true
  scene.add(platform)

  // 占位设备几何体（正式环境替换为 GLTFLoader 加载 .glb 模型）
  const geo = new THREE.BoxGeometry(1.0, 1.2, 0.6)
  const mat = new THREE.MeshStandardMaterial({ color: 0x79bbff, metalness: 0.6, roughness: 0.25 })
  cube = new THREE.Mesh(geo, mat)
  cube.position.y = 0.6
  cube.castShadow = true
  scene.add(cube)

  // 光照
  scene.add(new THREE.AmbientLight(0xffffff, 0.7))
  const dirLight = new THREE.DirectionalLight(0xffffff, 0.9)
  dirLight.position.set(3, 5, 3)
  dirLight.castShadow = true
  scene.add(dirLight)
  const backLight = new THREE.DirectionalLight(0xffffff, 0.3)
  backLight.position.set(-3, 2, -3)
  scene.add(backLight)

  // 动画循环
  const animate = () => {
    animationId = requestAnimationFrame(animate)
    controls.autoRotate = threeDAutoRotate.value
    controls.update()
    renderer.render(scene, camera)
  }
  animate()

  // 响应容器大小变化
  window.addEventListener('resize', () => {
    if (!container) return
    camera.aspect = container.clientWidth / container.clientHeight
    camera.updateProjectionMatrix()
    renderer.setSize(container.clientWidth, container.clientHeight)
  })
}

const destroyThreeScene = () => {
  cancelAnimationFrame(animationId)
  renderer?.dispose()
  if (threeContainerRef.value && renderer?.domElement) {
    threeContainerRef.value.removeChild(renderer.domElement)
  }
}

const resetThreeCamera = () => {
  camera.position.set(3, 2, 3)
  controls.reset()
}

/** 打开 3D 弹窗 */
const handleView3D = (row: EquipmentVO) => {
  threeDRow.value = row
  threeDAutoRotate.value = false
  threeDVisible.value = true
  nextTick(() => {
    if (threeContainerRef.value) initThreeScene(threeContainerRef.value)
  })
}

watch(threeDVisible, (val) => {
  if (!val) destroyThreeScene()
})

/** ========== CRUD 操作 ========== */
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
    purchaseDate: (row as any).purchaseDate || '',
    purchasePrice: (row as any).purchasePrice ?? undefined,
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

const handleStatus = async (row: EquipmentVO) => {
  if (row.status === 2) { ElMessage.warning('已报废设备无法更改状态'); return }
  const next = row.status === 1 ? 0 : 1
  const text = next === 1 ? '恢复正常' : '标记为维修'
  try {
    await ElMessageBox.confirm(`确认将此设备${text}？`, '提示', { type: 'warning' })
    await updateEquipmentStatus(row.id, next); ElMessage.success('状态更新成功'); fetchData()
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

.three-wrapper {
  display: flex;
  gap: 16px;
  height: 480px;
}

.three-canvas {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f2f5;
  border: 1px solid #e8e8e8;
  cursor: grab;
  &:active { cursor: grabbing; }
}

.three-sidebar {
  width: 220px;
  flex-shrink: 0;
  h4 { margin: 0 0 12px; font-size: 14px; color: #333; }
  .three-controls { display: flex; gap: 8px; margin-top: 12px; }
}
</style>
