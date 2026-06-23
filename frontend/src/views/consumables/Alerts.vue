<template>
  <div class="alerts-page">
    <!-- Summary cards -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #f3d19e);">
            <el-icon :size="28"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.lowStock ?? 0 }}</div>
            <div class="stat-label">低库存</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #fab6b6);">
            <el-icon :size="28"><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.expiring ?? 0 }}</div>
            <div class="stat-label">即将过期</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff);">
            <el-icon :size="28"><Box /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.overstock ?? 0 }}</div>
            <div class="stat-label">库存过量</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Alert list -->
    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>预警列表</span>
          <el-select v-model="filterType" placeholder="筛选类型" clearable style="width: 140px" @change="fetchAlerts">
            <el-option label="低库存" value="LOW_STOCK" />
            <el-option label="即将过期" value="EXPIRING" />
            <el-option label="库存过量" value="OVERSTOCK" />
          </el-select>
        </div>
      </template>

      <el-table :data="alertList" v-loading="loading" stripe border>
        <el-table-column prop="consumableName" label="耗材名称" min-width="140" />
        <el-table-column prop="type" label="预警类型" width="110">
          <template #default="{ row }">
            <el-tag :type="alertTypeTag(row.type)" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'CRITICAL' ? 'danger' : row.severity === 'HIGH' ? 'warning' : 'info'" size="small">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="预警信息" min-width="200" />
        <el-table-column prop="currentStock" label="当前库存" width="90" />
        <el-table-column prop="threshold" label="阈值" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 19) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="!row.acknowledged" size="small" type="primary" @click="handleAcknowledge(row)">确认</el-button>
            <el-tag v-else type="success" size="small">已确认</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchAlerts"
        @current-change="fetchAlerts"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, Timer, Box } from '@element-plus/icons-vue'
import { getStockAlerts, getAlertSummary, acknowledgeAlert } from '@/api/stockAlert'
import type { StockAlertVO } from '@/utils/types'

const alertTypeTag = (type: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = { LOW_STOCK: 'warning', EXPIRING: 'danger', OVERSTOCK: 'info' }
  return map[type] || 'info'
}

const summary = reactive<any>({})
const filterType = ref('')
const loading = ref(false)
const alertList = ref<StockAlertVO[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const fetchSummary = async () => {
  try {
    const res: any = await getAlertSummary()
    const data = res?.data || res
    Object.assign(summary, data)
  } catch {}
}

const fetchAlerts = async () => {
  loading.value = true
  try {
    const res: any = await getStockAlerts({
      pageNum: pagination.pageNum, pageSize: pagination.pageSize,
      type: filterType.value || undefined,
    })
    const data = res?.data || res
    alertList.value = data?.list || (Array.isArray(data) ? data : [])
    pagination.total = data?.total || 0
  } catch {} finally { loading.value = false }
}

const handleAcknowledge = async (row: StockAlertVO) => {
  try {
    await acknowledgeAlert(row.id)
    ElMessage.success('已确认'); fetchAlerts(); fetchSummary()
  } catch {}
}

onMounted(() => { fetchSummary(); fetchAlerts() })
</script>

<style scoped lang="scss">
.alerts-page { padding: 10px; }
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
