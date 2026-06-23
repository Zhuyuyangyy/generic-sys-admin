<template>
  <div class="audit-log-page">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="模块">
          <el-input v-model="searchForm.module" placeholder="模块名称" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="searchForm.username" placeholder="操作人" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作" min-width="140" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="duration" label="耗时(ms)" width="90" />
        <el-table-column prop="statusText" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170">
          <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 19) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">详情</el-button>
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

    <!-- Detail drawer -->
    <el-drawer v-model="drawerVisible" title="日志详情" size="450px">
      <el-descriptions v-if="currentLog" :column="1" border>
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.action }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentLog.statusText }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ currentLog.createTime?.replace('T', ' ').slice(0, 19) }}</el-descriptions-item>
        <el-descriptions-item label="参数" v-if="currentLog.params">
          <pre style="max-height: 200px; overflow: auto; font-size: 12px; margin: 0;">{{ currentLog.params }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentLog.errorMsg">
          <span style="color: #F56C6C;">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getOperationLogs } from '@/api/audit'
import type { OperationLogVO } from '@/utils/types'

const searchForm = reactive({ module: '', username: '' })
const dateRange = ref<string[]>([])
const loading = ref(false)
const tableData = ref<OperationLogVO[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const drawerVisible = ref(false)
const currentLog = ref<OperationLogVO | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getOperationLogs({
      pageNum: pagination.pageNum, pageSize: pagination.pageSize,
      module: searchForm.module || undefined,
      username: searchForm.username || undefined,
      startTime: dateRange.value?.[0] || undefined,
      endTime: dateRange.value?.[1] || undefined,
    })
    const data = res?.data || res
    tableData.value = data?.list || (Array.isArray(data) ? data : [])
    pagination.total = data?.total || 0
  } catch {} finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { searchForm.module = ''; searchForm.username = ''; dateRange.value = []; pagination.pageNum = 1; fetchData() }

const viewDetail = (row: OperationLogVO) => { currentLog.value = row; drawerVisible.value = true }

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.audit-log-page { display: flex; flex-direction: column; gap: 12px; padding: 10px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
