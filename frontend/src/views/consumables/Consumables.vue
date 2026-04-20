<template>
  <div class="consumables-page">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="耗材名称">
          <el-input v-model="searchForm.name" placeholder="请输入耗材名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="searchForm.category" placeholder="请选择类别" clearable style="width: 150px">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
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

    <!-- 表格区域 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>耗材列表</span>
          <div class="header-actions">
            <el-button type="success" @click="handleInbound">
              <el-icon><Bottom /></el-icon> 入库
            </el-button>
            <el-button type="warning" @click="handleOutbound">
              <el-icon><Top /></el-icon> 出库
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon> 新增耗材
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="耗材名称" min-width="140" />
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="currentStock" label="当前库存" width="90">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.currentStock <= row.minStockLevel }">
              {{ row.currentStock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="库存范围" width="120">
          <template #default="{ row }">
            {{ row.minStockLevel }} ~ {{ row.maxStockLevel }}
          </template>
        </el-table-column>
        <el-table-column prop="unitCost" label="单价(元)" width="90">
          <template #default="{ row }">
            {{ row.unitCost != null ? `¥${row.unitCost}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storageLocation" label="存放位置" min-width="120" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="info" @click="handleAdjust(row)">调整库存</el-button>
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
        <el-form-item label="耗材名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入耗材名称" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：个、箱、卷" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低库存" prop="minStockLevel">
          <el-input-number v-model="form.minStockLevel" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最高库存" prop="maxStockLevel">
          <el-input-number v-model="form.maxStockLevel" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价(元)" prop="unitCost">
          <el-input-number v-model="form.unitCost" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="存放位置">
          <el-input v-model="form.storageLocation" placeholder="请输入存放位置" />
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

    <!-- 入库弹窗 -->
    <el-dialog v-model="inboundVisible" title="耗材入库" width="420px">
      <el-form :model="inboundForm" label-width="80px">
        <el-form-item label="耗材">
          <el-select v-model="inboundForm.id" placeholder="请选择耗材" style="width: 100%" filterable>
            <el-option v-for="item in tableData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="inboundForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参考单号">
          <el-input v-model="inboundForm.referenceNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remarks" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundVisible = false">取消</el-button>
        <el-button type="success" :loading="ioLoading" @click="confirmInbound">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 出库弹窗 -->
    <el-dialog v-model="outboundVisible" title="耗材出库" width="420px">
      <el-form :model="outboundForm" label-width="80px">
        <el-form-item label="耗材">
          <el-select v-model="outboundForm.id" placeholder="请选择耗材" style="width: 100%" filterable>
            <el-option v-for="item in tableData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量">
          <el-input-number v-model="outboundForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参考单号">
          <el-input v-model="outboundForm.referenceNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="outboundForm.remarks" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outboundVisible = false">取消</el-button>
        <el-button type="warning" :loading="ioLoading" @click="confirmOutbound">确认出库</el-button>
      </template>
    </el-dialog>

    <!-- 库存调整弹窗 -->
    <el-dialog v-model="adjustVisible" title="库存调整" width="420px">
      <el-form :model="adjustForm" label-width="80px">
        <el-form-item label="耗材">
          <span>{{ (adjustForm as any).name || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ (adjustForm as any).currentStock ?? '-' }}</span>
        </el-form-item>
        <el-form-item label="调整量">
          <el-input-number v-model="adjustForm.delta" :min="-9999" style="width: 100%" placeholder="正数增加，负数减少" />
        </el-form-item>
        <el-form-item label="参考单号">
          <el-input v-model="(adjustForm as any).referenceNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="(adjustForm as any).remarks" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="ioLoading" @click="confirmAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 耗材管理视图
 * @description 耗材库存全生命周期管理页面，基于 CrudTemplate 模式实现搜索、分页、新增、编辑、软删除及入库/出库/库存调整
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Top, Bottom } from '@element-plus/icons-vue'
import {
  getConsumablePage,
  saveConsumable,
  updateConsumable,
  deleteConsumable,
  inboundConsumable,
  outboundConsumable,
  adjustConsumableStock,
  type ConsumableVO,
  type ConsumableSaveDTO,
} from '@/api/consumable'

const categoryOptions = ['办公用品', '生产耗材', '清洁用品', '包装材料', '其他']

/** 搜索表单响应式数据 */
const searchForm = reactive({ name: '', category: '' })
/** 分页参数 */
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const loading = ref(false)
const tableData = ref<ConsumableVO[]>([])

/** 弹窗状态 */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

/** 表单默认空数据 */
const defaultForm: ConsumableSaveDTO = {
  name: '',
  category: '',
  unit: '',
  minStockLevel: 0,
  maxStockLevel: 0,
  unitCost: 0,
  supplier: '',
  storageLocation: '',
  remarks: '',
}
const form = reactive<ConsumableSaveDTO>({ ...defaultForm })

/** 表单校验规则 */
const formRules = {
  name: [{ required: true, message: '请输入耗材名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  minStockLevel: [{ required: true, message: '请输入最低库存', trigger: 'blur' }],
  maxStockLevel: [{ required: true, message: '请输入最高库存', trigger: 'blur' }],
  unitCost: [{ required: true, message: '请输入单价', trigger: 'blur' }],
}

/** 入库/出库/调整弹窗状态 */
const inboundVisible = ref(false)
const outboundVisible = ref(false)
const adjustVisible = ref(false)
const ioLoading = ref(false)

const inboundForm = reactive({ id: undefined as number | undefined, quantity: 1, referenceNo: '', remarks: '' })
const outboundForm = reactive({ id: undefined as number | undefined, quantity: 1, referenceNo: '', remarks: '' })
const adjustForm = reactive({ id: undefined as number | undefined, delta: 0, currentStock: 0, name: '', referenceNo: '', remarks: '' } as any)

/**
 * 异步数据加载
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getConsumablePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined,
      category: searchForm.category || undefined,
    })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } catch {
    // 错误由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { searchForm.name = ''; searchForm.category = ''; pagination.pageNum = 1; fetchData() }

const handleAdd = () => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, defaultForm)
  dialogTitle.value = '新增耗材'; dialogVisible.value = true
}

const handleEdit = (row: ConsumableVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, {
    name: row.name, category: row.category, unit: row.unit,
    minStockLevel: row.minStockLevel, maxStockLevel: row.maxStockLevel,
    unitCost: row.unitCost, supplier: row.supplier || '',
    storageLocation: row.storageLocation || '', remarks: row.remarks || '',
  })
  dialogTitle.value = '编辑耗材'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateConsumable(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await saveConsumable(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleDelete = async (row: ConsumableVO) => {
  try {
    await ElMessageBox.confirm(`确认删除耗材「${row.name}」？`, '警告', { type: 'error', confirmButtonText: '删除' })
    await deleteConsumable(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消操作
  }
}

const handleInbound = () => {
  inboundForm.id = undefined; inboundForm.quantity = 1; inboundForm.referenceNo = ''; inboundForm.remarks = ''
  inboundVisible.value = true
}
const confirmInbound = async () => {
  if (!inboundForm.id) { ElMessage.warning('请选择耗材'); return }
  if (inboundForm.quantity < 1) { ElMessage.warning('数量至少为1'); return }
  ioLoading.value = true
  try {
    await inboundConsumable(inboundForm.id, inboundForm.quantity, inboundForm.referenceNo || undefined, inboundForm.remarks || undefined)
    ElMessage.success('入库成功')
    inboundVisible.value = false
    fetchData()
  } catch {} finally { ioLoading.value = false }
}

const handleOutbound = () => {
  outboundForm.id = undefined; outboundForm.quantity = 1; outboundForm.referenceNo = ''; outboundForm.remarks = ''
  outboundVisible.value = true
}
const confirmOutbound = async () => {
  if (!outboundForm.id) { ElMessage.warning('请选择耗材'); return }
  if (outboundForm.quantity < 1) { ElMessage.warning('数量至少为1'); return }
  ioLoading.value = true
  try {
    await outboundConsumable(outboundForm.id, outboundForm.quantity, outboundForm.referenceNo || undefined, outboundForm.remarks || undefined)
    ElMessage.success('出库成功')
    outboundVisible.value = false
    fetchData()
  } catch {} finally { ioLoading.value = false }
}

const handleAdjust = (row: ConsumableVO) => {
  adjustForm.id = row.id; adjustForm.delta = 0
  adjustForm.currentStock = row.currentStock; adjustForm.name = row.name
  adjustForm.referenceNo = ''; adjustForm.remarks = ''
  adjustVisible.value = true
}
const confirmAdjust = async () => {
  if (!adjustForm.id) return
  ioLoading.value = true
  try {
    await adjustConsumableStock(adjustForm.id, adjustForm.delta, adjustForm.referenceNo || undefined, adjustForm.remarks || undefined)
    ElMessage.success('库存调整成功')
    adjustVisible.value = false
    fetchData()
  } catch {} finally { ioLoading.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.consumables-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .header-actions { display: flex; gap: 8px; }
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.low-stock { color: #f56c6c; font-weight: bold; }
</style>
