<template>
  <div class="dashboard">
    <!-- Top row: 4 stat cards -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff);">
            <el-icon :size="28"><Monitor /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalEquipment }}</div>
            <div class="stat-label">总设备数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475);">
            <el-icon :size="28"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.normalEquipment }}</div>
            <div class="stat-label">正常设备</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="router.push('/consumables/alerts')">
          <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #f3d19e);">
            <el-icon :size="28"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.lowStockConsumables }}</div>
            <div class="stat-label">低库存耗材</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="router.push('/workflow/my-tasks')">
          <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #fab6b6);">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.pendingTasks }}</div>
            <div class="stat-label">待审批工单</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Second row: Charts -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>设备状态分布</span>
              <el-button text size="small" @click="fetchStatusDistribution">刷新</el-button>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 300px;" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>库存汇总</span>
              <el-button text size="small" @click="fetchInventorySummary">刷新</el-button>
            </div>
          </template>
          <div ref="barChartRef" style="height: 300px;" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Third row: Recent activities + Real-time events -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近活动</span>
            </div>
          </template>
          <el-timeline v-if="recentActivities.length > 0">
            <el-timeline-item
              v-for="item in recentActivities"
              :key="item.id"
              :timestamp="formatTime(item.timestamp)"
              placement="top"
              :type="activityType(item.type)"
            >
              <div class="activity-item">
                <span class="activity-action">{{ item.action }}</span>
                <span class="activity-desc">{{ item.description }}</span>
                <span v-if="item.username" class="activity-user">{{ item.username }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无活动记录" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>实时事件</span>
              <el-tag :type="wsConnected ? 'success' : 'danger'" size="small">
                {{ wsConnected ? '已连接' : '未连接' }}
              </el-tag>
            </div>
          </template>
          <div class="event-feed" v-if="events.length > 0">
            <div v-for="(evt, idx) in events" :key="idx" class="event-item">
              <el-tag size="small" :type="eventTypeTag(evt.type)">{{ evt.type }}</el-tag>
              <span class="event-msg">{{ evt.message || evt.description }}</span>
              <span class="event-time">{{ formatTime(evt.createTime || evt.timestamp) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无实时事件" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom row: Maintenance upcoming + Stock alerts -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>即将到期维保</span>
              <el-button text size="small" @click="fetchUpcomingMaintenance">刷新</el-button>
            </div>
          </template>
          <el-table :data="upcomingMaintenance" stripe size="small" :empty-text="'暂无数据'">
            <el-table-column prop="title" label="计划名称" min-width="140" />
            <el-table-column prop="equipmentName" label="设备" width="120" />
            <el-table-column prop="scheduledDate" label="计划日期" width="110">
              <template #default="{ row }">{{ row.scheduledDate?.slice(0, 10) }}</template>
            </el-table-column>
            <el-table-column prop="statusText" label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'COMPLETED' ? 'success' : row.status === 'OVERDUE' ? 'danger' : 'warning'">
                  {{ row.statusText }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>库存预警汇总</span>
              <el-button text size="small" @click="fetchAlertSummary">刷新</el-button>
            </div>
          </template>
          <el-descriptions :column="2" border size="small" v-if="alertSummary">
            <el-descriptions-item label="低库存">{{ alertSummary.lowStock ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="即将过期">{{ alertSummary.expiring ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="库存过量">{{ alertSummary.overstock ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="总计预警">{{ alertSummary.total ?? 0 }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无预警数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Monitor, CircleCheck, Warning, Document } from '@element-plus/icons-vue'
import { getOverview, getEquipmentStatus, getInventorySummary, getRecentActivities } from '@/api/dashboard'
import { getRecentEvents } from '@/api/events'
import { getUpcomingMaintenance } from '@/api/maintenance'
import { getAlertSummary } from '@/api/stockAlert'
import type { DashboardOverview, EquipmentStatusDistribution, InventorySummary, RecentActivity } from '@/utils/types'

const router = useRouter()

const overview = reactive<DashboardOverview>({
  totalEquipment: 0, normalEquipment: 0, maintenanceEquipment: 0, scrappedEquipment: 0,
  totalConsumables: 0, lowStockConsumables: 0, pendingMaintenancePlans: 0,
  overdueMaintenancePlans: 0, activeWorkflowInstances: 0, pendingTasks: 0,
})

const recentActivities = ref<RecentActivity[]>([])
const events = ref<any[]>([])
const upcomingMaintenance = ref<any[]>([])
const alertSummary = ref<any>(null)
const wsConnected = ref(false)

const pieChartRef = ref()
const barChartRef = ref()
let pieChart: echarts.ECharts
let barChart: echarts.ECharts
let refreshTimer: ReturnType<typeof setInterval>
let ws: WebSocket | null = null

const formatTime = (ts: string) => ts ? ts.replace('T', ' ').slice(0, 19) : '-'
const activityType = (type: string): 'primary' | 'success' | 'warning' | 'danger' => {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger'> = {
    EQUIPMENT: 'primary', CONSUMABLE: 'warning', MAINTENANCE: 'danger', WORKFLOW: 'success',
  }
  return map[type] || 'primary'
}
const eventTypeTag = (type: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    ALERT: 'danger', INFO: 'info', UPDATE: 'success',
  }
  return map[type] || 'info'
}

const fetchOverview = async () => {
  try {
    const data = await getOverview()
    Object.assign(overview, data)
  } catch {}
}

const fetchStatusDistribution = async () => {
  try {
    const data: EquipmentStatusDistribution = await getEquipmentStatus()
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, textStyle: { fontSize: 13 } },
      series: [{
        type: 'pie', radius: ['42%', '72%'], center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c}台', fontSize: 13 },
        emphasis: { itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,0.2)' } },
        data: [
          { name: '正常', value: data.normal, itemStyle: { color: '#67C23A' } },
          { name: '维修中', value: data.maintenance, itemStyle: { color: '#E6A23C' } },
          { name: '报废', value: data.scrapped, itemStyle: { color: '#F56C6C' } },
        ],
      }],
    })
  } catch {}
}

const fetchInventorySummary = async () => {
  try {
    const data: InventorySummary = await getInventorySummary()
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['数量'], top: 0 },
      grid: { left: 40, right: 20, bottom: 30, top: 40 },
      xAxis: {
        type: 'category',
        data: ['总类别', '总项数', '低库存', '缺货', '即将过期'],
      },
      yAxis: { type: 'value' },
      series: [{
        name: '数量', type: 'bar', barWidth: 36,
        data: [
          { value: data.totalCategories, itemStyle: { color: '#409EFF' } },
          { value: data.totalItems, itemStyle: { color: '#67C23A' } },
          { value: data.lowStockItems, itemStyle: { color: '#E6A23C' } },
          { value: data.outOfStockItems, itemStyle: { color: '#F56C6C' } },
          { value: data.expiringItems, itemStyle: { color: '#E6A23C' } },
        ],
        itemStyle: { borderRadius: [4, 4, 0, 0] },
      }],
    })
  } catch {}
}

const fetchRecentActivities = async () => {
  try {
    recentActivities.value = await getRecentActivities()
  } catch {}
}

const fetchEvents = async () => {
  try {
    const res: any = await getRecentEvents()
    events.value = Array.isArray(res) ? res : res?.data || []
  } catch {}
}

const fetchUpcomingMaintenance = async () => {
  try {
    const res: any = await getUpcomingMaintenance(7)
    upcomingMaintenance.value = Array.isArray(res) ? res : res?.data?.list || res?.data || []
  } catch {}
}

const fetchAlertSummary = async () => {
  try {
    const res: any = await getAlertSummary()
    alertSummary.value = res?.data || res
  } catch {}
}

const connectWebSocket = () => {
  try {
    const wsUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/events`
    ws = new WebSocket(wsUrl)
    ws.onopen = () => { wsConnected.value = true }
    ws.onclose = () => { wsConnected.value = false; setTimeout(connectWebSocket, 5000) }
    ws.onerror = () => { wsConnected.value = false }
    ws.onmessage = (e) => {
      try {
        const evt = JSON.parse(e.data)
        events.value.unshift(evt)
        if (events.value.length > 50) events.value.pop()
      } catch {}
    }
  } catch {
    wsConnected.value = false
  }
}

const refreshAll = () => {
  fetchOverview()
  fetchStatusDistribution()
  fetchInventorySummary()
  fetchRecentActivities()
  fetchEvents()
  fetchUpcomingMaintenance()
  fetchAlertSummary()
}

onMounted(() => {
  pieChart = echarts.init(pieChartRef.value)
  barChart = echarts.init(barChartRef.value)
  refreshAll()
  connectWebSocket()
  refreshTimer = setInterval(refreshAll, 30000)
  window.addEventListener('resize', () => { pieChart.resize(); barChart.resize() })
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  ws?.close()
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard { padding: 10px; }

.stat-card {
  display: flex; align-items: center; cursor: pointer; transition: transform 0.2s;
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

.activity-item {
  .activity-action { font-weight: 500; color: #333; margin-right: 8px; }
  .activity-desc { color: #666; font-size: 13px; }
  .activity-user { color: #409EFF; font-size: 12px; margin-left: 8px; }
}

.event-feed {
  max-height: 300px; overflow-y: auto;
  .event-item {
    display: flex; align-items: center; gap: 8px;
    padding: 8px 0; border-bottom: 1px solid #f0f0f0;
    .event-msg { flex: 1; font-size: 13px; color: #333; }
    .event-time { font-size: 12px; color: #999; flex-shrink: 0; }
  }
}
</style>
