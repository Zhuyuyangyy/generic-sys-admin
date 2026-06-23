<template>
  <div class="anomaly-page">
    <!-- Summary cards -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #fab6b6);">
            <el-icon :size="28"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.bySeverity?.CRITICAL ?? 0 }}</div>
            <div class="stat-label">严重</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #f3d19e);">
            <el-icon :size="28"><InfoFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.bySeverity?.HIGH ?? 0 }}</div>
            <div class="stat-label">高</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff);">
            <el-icon :size="28"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.bySeverity?.MEDIUM ?? 0 }}</div>
            <div class="stat-label">中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475);">
            <el-icon :size="28"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.bySeverity?.LOW ?? 0 }}</div>
            <div class="stat-label">低</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>异常事件列表</span>
          <el-button type="primary" @click="handleScan" :loading="scanning"><el-icon><Refresh /></el-icon> 手动扫描</el-button>
        </div>
      </template>

      <el-table :data="anomalies" v-loading="loading" stripe border>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :color="severityColor(row.severity)" style="color: #fff; border: none;" size="small">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" />
        <el-table-column prop="username" label="关联用户" width="100" />
        <el-table-column label="已解决" width="80">
          <template #default="{ row }">
            <el-tag :type="row.resolved ? 'success' : 'danger'" size="small">{{ row.resolved ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发生时间" width="160">
          <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 19) || '-' }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchAnomalies"
        @current-change="fetchAnomalies"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, InfoFilled, CircleCheck, Refresh } from '@element-plus/icons-vue'
import { getAnomalies, getAnomalySummary, scanAnomalies } from '@/api/audit'
import type { AnomalyEvent, AnomalySummary } from '@/utils/types'

const severityColor = (s: string) => {
  const map: Record<string, string> = { CRITICAL: '#8B0000', HIGH: '#F56C6C', MEDIUM: '#E6A23C', LOW: '#67C23A' }
  return map[s] || '#909399'
}

const summary = reactive<AnomalySummary>({ total: 0, unresolved: 0, byType: {}, bySeverity: {}, recentCount: 0 })
const loading = ref(false)
const anomalies = ref<AnomalyEvent[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const scanning = ref(false)

const fetchSummary = async () => {
  try {
    const data = await getAnomalySummary()
    Object.assign(summary, data)
  } catch {}
}

const fetchAnomalies = async () => {
  loading.value = true
  try {
    const res: any = await getAnomalies({ pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    const data = res?.data || res
    anomalies.value = data?.list || (Array.isArray(data) ? data : [])
    pagination.total = data?.total || 0
  } catch {} finally { loading.value = false }
}

const handleScan = async () => {
  scanning.value = true
  try {
    await scanAnomalies()
    ElMessage.success('扫描完成'); fetchSummary(); fetchAnomalies()
  } catch {} finally { scanning.value = false }
}

onMounted(() => { fetchSummary(); fetchAnomalies() })
</script>

<style scoped lang="scss">
.anomaly-page { padding: 10px; }
.stat-card {
  display: flex; align-items: center; transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
  .stat-icon {
    width: 56px; height: 56px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    color: #fff; margin-right: 16px; flex-shrink: 0;
  }
  .stat-info {
    .stat-value { font-size: 24px; font-weight: bold; color: #333; }
    .stat-label { font-size: 13px; color: #888; margin-top: 4px; }
  }
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
