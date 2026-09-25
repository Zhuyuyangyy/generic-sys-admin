<template>
  <div class="logs-page">
    <!-- 查询条件卡片 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作模块">
          <el-input v-model="searchForm.module" placeholder="请输入模块名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-input v-model="searchForm.operation" placeholder="请输入操作类型" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="searchForm.operator" placeholder="请输入用户名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作结果">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="成功" value="1" />
            <el-option label="失败" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>操作日志列表</span>
          <span class="total-hint">共 {{ total }} 条记录</span>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        :empty-text="loading ? '加载中...' : '暂无数据'"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="module" label="操作模块" min-width="120" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作类型" width="120" show-overflow-tooltip />
        <el-table-column prop="methodName" label="方法名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="username" label="操作人" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.username }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column prop="durationMs" label="耗时" width="90" align="center">
          <template #default="{ row }">
            <span :class="durationClass(row.durationMs)">{{ row.durationMs }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="resultStatus" label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.resultStatus === 1 ? 'success' : 'danger'" size="small">
              {{ row.resultStatus === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationTime" label="操作时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.operationTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button text size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="操作模块" :span="2">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog.operation }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ currentLog.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="目标表">{{ currentLog.targetTable || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标ID">{{ currentLog.targetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.durationMs }}ms</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatTime(currentLog.operationTime) }}</el-descriptions-item>
        <el-descriptions-item label="操作结果">
          <el-tag :type="currentLog.resultStatus === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.resultStatus === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="params-pre">{{ formatJson(currentLog.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog.errorDetail" label="错误详情" :span="2">
          <pre class="params-pre error">{{ currentLog.errorDetail }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">
          <span class="user-agent-text">{{ currentLog.userAgent }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 操作日志页面
 */
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getOperationLogs, type SysOperationLogVO, type LogQueryDTO } from '@/api/log'

const loading = ref(false)
const tableData = ref<SysOperationLogVO[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])

const searchForm = reactive({
  module: '',
  operation: '',
  operator: '',
  status: '',
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
})

const detailVisible = ref(false)
const currentLog = ref<SysOperationLogVO | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    const params: LogQueryDTO = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      module: searchForm.module || undefined,
      operation: searchForm.operation || undefined,
      operator: searchForm.operator || undefined,
      status: searchForm.status || undefined,
      startTime: dateRange.value?.[0] || undefined,
      endTime: dateRange.value?.[1] || undefined,
    }
    const res: any = await getOperationLogs(params)
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.module = ''
  searchForm.operation = ''
  searchForm.operator = ''
  searchForm.status = ''
  dateRange.value = []
  handleSearch()
}

const showDetail = (row: SysOperationLogVO) => {
  currentLog.value = row
  detailVisible.value = true
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const formatJson = (str: string) => {
  if (!str) return '-'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

const durationClass = (ms: number) => {
  if (ms > 1000) return 'duration-danger'
  if (ms > 300) return 'duration-warning'
  return 'duration-ok'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.logs-page {
  .search-card {
    margin-bottom: 16px;
    :deep(.el-card__body) { padding-bottom: 0; }
  }

  .table-card {
    :deep(.el-card__header) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    .total-hint {
      font-size: 12px;
      color: #909399;
      font-weight: normal;
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .duration-ok { color: #67C23A; }
  .duration-warning { color: #E6A23C; font-weight: 600; }
  .duration-danger { color: #F56C6C; font-weight: 600; }

  .params-pre {
    background: #f5f7fa;
    padding: 8px 12px;
    border-radius: 4px;
    font-size: 12px;
    max-height: 200px;
    overflow: auto;
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
    &.error {
      color: #F56C6C;
      background: #fef0f0;
    }
  }

  .user-agent-text {
    font-size: 11px;
    color: #909399;
    word-break: break-all;
  }
}
</style>