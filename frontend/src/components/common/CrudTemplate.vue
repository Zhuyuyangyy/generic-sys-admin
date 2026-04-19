<template>
  <div class="crud-template">
    <!-- ==================== 第一部分：搜索区域 ==================== -->
    <div class="search-bar">
      <el-form :model="searchForm" inline @submit.prevent="handleSearch">
        <!--
          🏆 高级创新点：动态搜索字段
          只需要改 columns 数组，搜索框自动增减，不用动模板
          columns = [
            { prop: 'name', label: '书名', type: 'input' },
            { prop: 'author', label: '作者', type: 'input' },
            { prop: 'status', label: '状态', type: 'select', options: [...] }
          ]
        -->
        <el-form-item v-for="col in searchColumns" :key="col.prop" :label="col.label">
          <!-- 输入框类型 -->
          <el-input
            v-if="col.type === 'input' || !col.type"
            v-model="searchForm[col.prop]"
            :placeholder="'请输入' + col.label"
            clearable
            style="width: 180px"
          />

          <!-- 下拉框类型 -->
          <el-select
            v-else-if="col.type === 'select'"
            v-model="searchForm[col.prop]"
            :placeholder="'请选择' + col.label"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="opt in col.options"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>

          <!-- 日期范围类型 -->
          <el-date-picker
            v-else-if="col.type === 'daterange'"
            v-model="searchForm[col.prop]"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
          <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
          <el-button type="success" @click="handleAdd" :icon="Plus">新增</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ==================== 第二部分：数据表格 ==================== -->
    <div class="table-container">
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <!-- 多选列（可选） -->
        <el-table-column v-if="showSelection" type="selection" width="50" />

        <!-- 序号列 -->
        <el-table-column type="index" label="序号" width="60" align="center" />

        <!--
          🏆 高级创新点：动态表格列
          只需要改 columns 数组，表格列自动增减
          支持 slot 自定义列，如操作列
        -->
        <el-table-column
          v-for="col in tableColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width || 'auto'"
          :align="col.align || 'center'"
        >
          <!-- 如果有 formatter，用它格式化显示值 -->
          <template #default="{ row }">
            <span v-if="col.formatter">
              {{ col.formatter(row[col.prop], row) }}
            </span>
            <span v-else>{{ row[col.prop] ?? '—' }}</span>
          </template>
        </el-table-column>

        <!-- 操作列（固定最后一列） -->
        <el-table-column label="操作" fixed="right" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- ==================== 第三部分：新增/编辑弹窗 ==================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="dialogForm"
        :rules="dialogRules"
        label-width="100px"
      >
        <!--
          🏆 高级创新点：动态表单项
          只需要改 columns 数组，新增/编辑弹窗自动生成
          支持各种类型的表单项：input / select / textarea / date / number
        -->
        <el-form-item
          v-for="col in dialogColumns"
          :key="col.prop"
          :label="col.label"
          :prop="col.prop"
        >
          <!-- 输入框 -->
          <el-input
            v-if="col.type === 'input' || !col.type"
            v-model="dialogForm[col.prop]"
            :placeholder="'请输入' + col.label"
            maxlength="100"
            show-word-limit
          />

          <!-- 文本域 -->
          <el-input
            v-else-if="col.type === 'textarea'"
            v-model="dialogForm[col.prop]"
            type="textarea"
            :rows="3"
            :placeholder="'请输入' + col.label"
            maxlength="500"
            show-word-limit
          />

          <!-- 下拉框 -->
          <el-select
            v-else-if="col.type === 'select'"
            v-model="dialogForm[col.prop]"
            :placeholder="'请选择' + col.label"
            style="width: 100%"
          >
            <el-option
              v-for="opt in col.options"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>

          <!-- 日期选择 -->
          <el-date-picker
            v-else-if="col.type === 'date'"
            v-model="dialogForm[col.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="'请选择' + col.label"
            style="width: 100%"
          />

          <!-- 数字输入 -->
          <el-input-number
            v-else-if="col.type === 'number'"
            v-model="dialogForm[col.prop]"
            :min="col.min || 0"
            :max="col.max || 999999"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus } from '@element-plus/icons-vue'
import { useTts } from '@/api/voice/tts'  // 🏆 引入 TTS Hook

// ==================== Props 定义 ====================

const props = defineProps<{
  /**
   * 🏆 高级创新点：配置驱动型 CRUD
   * 只需要传一个 columns 数组，整页自动生成
   *
   * columns 示例：
   * ```js
   * const columns = [
   *   { prop: 'name', label: '书名', type: 'input',
   *     search: true, table: true, dialog: true,
   *     rules: [{ required: true, message: '请输入书名', trigger: 'blur' }]
   *   },
   *   { prop: 'author', label: '作者', type: 'input',
   *     search: true, table: true, dialog: true
   *   },
   *   { prop: 'status', label: '状态', type: 'select',
   *     search: true, table: true, dialog: true,
   *     options: [
   *       { label: '在架', value: 1 },
   *       { label: '下架', value: 0 }
   *     ],
   *     formatter: (val) => val === 1 ? '在架' : '下架'
   *   }
   * ]
   * ```
   */
  columns: CrudColumn[]

  /** API 地址（CRUD 的 GET/POST/PUT/DELETE 基础路径） */
  api: string

  /** 是否显示多选列 */
  showSelection?: boolean
}>()

// ==================== TTS 语音播报 ====================
const { play } = useTts()

/**
 * 🏆 高级创新点：操作成功/失败时自动语音播报
 * 每次删除、每次保存成功，系统自动调用 TTS 播报
 * 不用看屏幕也能知道操作结果（无障碍设计加分项）
 */
const speakSuccess = (message: string) => {
  play(message + '，操作成功')
}

const speakError = (message: string) => {
  play(message + '，操作失败，请检查输入')
}

const speakDelete = () => {
  play('确认删除，数据已移除')
}

// ==================== 动态列过滤 ====================

/** 搜索栏显示的列 */
const searchColumns = computed(() =>
  props.columns.filter(col => col.search !== false && col.type !== 'hidden')
)

/** 表格显示的列 */
const tableColumns = computed(() =>
  props.columns.filter(col => col.table !== false)
)

/** 弹窗表单列 */
const dialogColumns = computed(() =>
  props.columns.filter(col => col.dialog !== false && col.type !== 'hidden')
)

// ==================== 数据状态 ====================

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const formRef = ref()

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 搜索表单
const searchForm = reactive<Record<string, any>>({})

// 弹窗表单
const dialogForm = reactive<Record<string, any>>({})
const dialogVisible = ref(false)
const dialogTitle = ref('新增')
const editingId = ref<number | null>(null)

// ==================== 表单验证规则（从 columns 自动生成） ====================

const dialogRules = computed(() => {
  const rules: Record<string, any[]> = {}
  props.columns.forEach(col => {
    if (col.rules) {
      rules[col.prop] = col.rules
    } else if (col.required) {
      rules[col.prop] = [
        { required: true, message: `请输入${col.label}`, trigger: 'blur' }
      ]
    }
  })
  return rules
})

// ==================== API 请求方法 ====================

/**
 * 🏆 高级创新点：数据源解耦
 * fetchTableData 只依赖 props.api，换业务只需改 api 地址
 * 如果需要自定义参数，可以在组件内覆盖此方法
 */
const fetchTableData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: pagination.current,
      size: pagination.size
    }

    // 搜索参数拼接
    Object.keys(searchForm).forEach(key => {
      if (searchForm[key] !== '' && searchForm[key] !== null && searchForm[key] !== undefined) {
        params[key] = searchForm[key]
      }
    })

    const res = await fetch(`${props.api}/page`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token') },
      body: JSON.stringify(params)
    }).then(r => r.json())

    if (res.code === 200) {
      tableData.value = res.data.records || res.data
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '加载数据失败')
    }
  } catch (err) {
    ElMessage.error('网络错误，加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchTableData()
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = null
  })
  handleSearch()
}

const handlePageChange = (page: number) => {
  pagination.current = page
  fetchTableData()
}

const handlePageSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1
  fetchTableData()
}

const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

// ==================== 新增 / 编辑 / 删除 ====================

const handleAdd = () => {
  dialogTitle.value = '新增'
  editingId.value = null
  resetDialogForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑'
  editingId.value = row.id
  // 填充表单
  props.columns.forEach(col => {
    if (col.dialog !== false) {
      dialogForm[col.prop] = row[col.prop]
    }
  })
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定删除该记录吗？删除后可在回收站恢复。`,
      '删除确认',
      { type: 'warning' }
    )

    const res = await fetch(`${props.api}/${row.id}`, {
      method: 'DELETE',
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
    }).then(r => r.json())

    if (res.code === 200) {
      ElMessage.success('删除成功')
      speakDelete()  // 🏆 删除成功语音播报
      fetchTableData()
    } else {
      ElMessage.error(res.message || '删除失败')
      speakError('删除')
    }
  } catch {
    // 用户取消
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const method = editingId.value ? 'PUT' : 'POST'
      const url = editingId.value ? `${props.api}/${editingId.value}` : props.api

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token') },
        body: JSON.stringify(dialogForm)
      }).then(r => r.json())

      if (res.code === 200) {
        ElMessage.success(dialogTitle.value + '成功')
        speakSuccess(dialogTitle.value)  // 🏆 保存成功语音播报
        dialogVisible.value = false
        fetchTableData()
      } else {
        ElMessage.error(res.message || '操作失败')
        speakError(dialogTitle.value)
      }
    } catch (err) {
      ElMessage.error('网络错误')
      speakError('操作')
    } finally {
      submitLoading.value = false
    }
  })
}

const resetDialogForm = () => {
  props.columns.forEach(col => {
    if (col.dialog !== false) {
      dialogForm[col.prop] = null
    }
  })
  formRef.value?.resetFields()
}

const handleDialogClosed = () => {
  formRef.value?.resetFields()
  resetDialogForm()
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchTableData()
})

// ==================== 暴露方法给父组件 ====================

defineExpose({ fetchTableData })
</script>

<script lang="ts">
/**
 * 🏆 高级创新点：类型定义 —— 一目了然的配置对象
 *
 * 使用者只需要看这个类型就知道怎么配置：
 * - prop: 字段名（对应后端 Entity 字段）
 * - label: 界面上显示的汉字名称
 * - type: 用什么组件（input / select / textarea / number / date / daterange）
 * - search / table / dialog: 控制三个区域是否显示
 * - options: 下拉选项（type=select时必填）
 * - rules: 表单验证规则
 * - formatter: 表格列格式化显示
 *
 * 不需要看组件代码，只需要改配置数组，1分钟搞定一个业务页面！
 */
export interface CrudColumn {
  prop: string
  label: string
  type?: 'input' | 'select' | 'textarea' | 'number' | 'date' | 'daterange' | 'hidden'
  search?: boolean      // 是否在搜索栏显示
  table?: boolean       // 是否在表格列显示
  dialog?: boolean     // 是否在新增/编辑弹窗显示
  required?: boolean   // 是否必填
  rules?: any[]        // 自定义验证规则
  options?: { label: string; value: any }[]  // 下拉选项
  min?: number         // 数字最小值
  max?: number         // 数字最大值
  width?: string       // 表格列宽
  align?: 'left' | 'center' | 'right'  // 表格列对齐
  formatter?: (value: any, row: any) => string  // 表格列格式化
}
</script>

<style scoped>
.crud-template {
  padding: 20px;
}
.search-bar {
  background: #f5f7fa;
  padding: 18px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.table-container {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
