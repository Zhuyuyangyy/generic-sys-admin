<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff);">
            <el-icon :size="28"><Box /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.equipment?.total || 0 }}</div>
            <div class="stat-label">设备总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475);">
            <el-icon :size="28"><Goods /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.consumable?.total || 0 }}</div>
            <div class="stat-label">耗材种类</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="router.push('/consumables')">
          <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #f3d19e);">
            <el-icon :size="28"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.consumable?.lowStock || 0 }}</div>
            <div class="stat-label">库存预警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #fab6b6);">
            <el-icon :size="28"><Tools /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.equipment?.maintenance || 0 }}</div>
            <div class="stat-label">维修中设备</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <!-- 设备状态分布 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>设备状态分布</span>
              <el-button text size="small" @click="loadStats">刷新</el-button>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 300px;" />
        </el-card>
      </el-col>

      <!-- 操作日志趋势 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>近7日操作日志</span>
            </div>
          </template>
          <div ref="barChartRef" style="height: 300px;" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 设备状态 + 操作类型 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>操作类型分布</span>
            </div>
          </template>
          <div ref="operationChartRef" style="height: 280px;" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>耗材预警信息</span>
              <el-button text size="small" @click="loadStats">刷新</el-button>
            </div>
          </template>
          <div class="summary-stats">
            <div class="summary-item">
              <span class="summary-num warning">{{ stats.consumable?.lowStock || 0 }}</span>
              <span class="summary-text">库存不足</span>
            </div>
            <div class="summary-item">
              <span class="summary-num danger">{{ stats.consumable?.expiring || 0 }}</span>
              <span class="summary-text">30天内过期</span>
            </div>
            <div class="summary-item">
              <span class="summary-num">{{ stats.user?.total || 0 }}</span>
              <span class="summary-text">用户总数</span>
            </div>
            <div class="summary-item">
              <span class="summary-num success">{{ stats.user?.active || 0 }}</span>
              <span class="summary-text">活跃用户</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
/**
 * 数据大屏视图（仪表盘）- 聚合版
 * @description 调用后端 /api/dashboard/stats 聚合接口，前端不再拉全量明细
 */
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Box, Goods, Warning, Tools } from '@element-plus/icons-vue'
import { getDashboardStats, type DashboardStatsVO } from '@/api/dashboard'

const router = useRouter()

/** 统计数据（从后端聚合获取） */
const stats = reactive<DashboardStatsVO>({
  equipment: { total: 0, normal: 0, maintenance: 0, scrapped: 0 },
  consumable: { total: 0, lowStock: 0, expiring: 0 },
  user: { total: 0, active: 0, disabled: 0 },
  logTrend: [],
  equipmentStatus: [],
  operationType: [],
})

/** 图表引用 */
const pieChartRef = ref()
const barChartRef = ref()
const operationChartRef = ref()
let pieChart: echarts.ECharts
let barChart: echarts.ECharts
let operationChart: echarts.ECharts

/** 加载统计数据 */
const loadStats = async () => {
  try {
    const res: any = await getDashboardStats()
    // 合并到响应式对象
    Object.assign(stats, res)
    renderCharts()
  } catch {
    // silent fail
  }
}

/** 渲染所有图表 */
const renderCharts = () => {
  renderPieChart()
  renderBarChart()
  renderOperationChart()
}

/** 设备状态饼图 */
const renderPieChart = () => {
  if (!pieChart) return
  const data = stats.equipmentStatus?.length
    ? stats.equipmentStatus
    : [
        { label: '正常', value: stats.equipment?.normal || 0 },
        { label: '维护中', value: stats.equipment?.maintenance || 0 },
        { label: '已报废', value: stats.equipment?.scrapped || 0 },
      ]
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 13 } },
    color: ['#67C23A', '#E6A23C', '#F56C6C'],
    series: [{
      type: 'pie',
      radius: ['42%', '72%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}', fontSize: 13 },
      emphasis: {
        itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,0.2)' },
        label: { fontSize: 15, fontWeight: 'bold' },
      },
      data,
    }],
  })
}

/** 操作日志趋势柱状图 */
const renderBarChart = () => {
  if (!barChart) return
  const data = stats.logTrend || []
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: data.map((d: { date: string }) => d.date.slice(5)), // MM-DD
      axisLabel: { fontSize: 11, color: '#888' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 11, color: '#888' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [{
      type: 'bar',
      data: data.map((d: { count: number }) => d.count),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' },
        ]),
        borderRadius: [4, 4, 0, 0],
      },
      barWidth: '50%',
    }],
  })
}

/** 操作类型分布饼图 */
const renderOperationChart = () => {
  if (!operationChart) return
  const data = stats.operationType || []
  operationChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 }, itemWidth: 14 },
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#B37FEB'],
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      label: { show: true, formatter: '{b}: {d}%', fontSize: 11 },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.15)' },
        label: { fontSize: 13, fontWeight: 'bold' },
      },
      data,
    }],
  })
}

/** 初始化图表 */
const initCharts = () => {
  pieChart = echarts.init(pieChartRef.value)
  barChart = echarts.init(barChartRef.value)
  operationChart = echarts.init(operationChartRef.value)
}

let resizeHandler: () => void

onMounted(() => {
  initCharts()
  loadStats()
  resizeHandler = () => {
    pieChart?.resize()
    barChart?.resize()
    operationChart?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 10px;
}

.stat-card {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
  .stat-icon {
    width: 56px; height: 56px;
    border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    color: #fff; margin-right: 16px;
    flex-shrink: 0;
  }
  .stat-info {
    .stat-value { font-size: 24px; font-weight: bold; color: #333; }
    .stat-label { font-size: 13px; color: #888; margin-top: 4px; }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  padding: 8px 0;

  .summary-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;

    .summary-num {
      font-size: 28px;
      font-weight: bold;
      color: #333;

      &.warning { color: #E6A23C; }
      &.danger { color: #F56C6C; }
      &.success { color: #67C23A; }
    }

    .summary-text {
      font-size: 13px;
      color: #888;
    }
  }
}
</style>