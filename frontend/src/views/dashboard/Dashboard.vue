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
            <div class="stat-value">{{ stats.equipmentTotal }}</div>
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
            <div class="stat-value">{{ stats.consumableTotal }}</div>
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
            <div class="stat-value">{{ stats.lowStockCount }}</div>
            <div class="stat-label">库存预警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #fab6b6);">
            <el-icon :size="28"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.maintenanceCount }}</div>
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
              <el-button text size="small" @click="fetchEquipmentChart">刷新</el-button>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 300px;" />
        </el-card>
      </el-col>

      <!-- 库存预警仪表盘 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>耗材库存预警</span>
              <el-button text size="small" @click="fetchConsumableChart">刷新</el-button>
            </div>
          </template>
          <div ref="gaugeChartRef" style="height: 300px;" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存预警列表 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>⚠️ 库存异常详情</span>
            </div>
          </template>
          <el-table :data="lowStockItems" stripe :empty-text="lowStockItems.length === 0 ? '暂无预警' : ''">
            <el-table-column prop="name" label="耗材名称" min-width="160" />
            <el-table-column prop="category" label="类别" width="120" />
            <el-table-column prop="currentStock" label="当前库存" width="100">
              <template #default="{ row }">
                <span :class="stockClass(row)">{{ row.currentStock }} {{ row.unit }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="minStockLevel" label="最低库存" width="100" />
            <el-table-column prop="maxStockLevel" label="最高库存" width="100" />
            <el-table-column label="库存健康度" width="220">
              <template #default="{ row }">
                <el-progress
                  :percentage="stockPercentage(row)"
                  :color="stockColor(row)"
                  :stroke-width="10"
                />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="stockTagType(row)" size="small">{{ stockStatus(row) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
/**
 * 数据大屏视图（仪表盘）
 * @description 基于 ECharts 的数据可视化大屏，展示设备状态分布、库存预警等关键业务指标
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Box, Goods, Warning, CircleClose } from '@element-plus/icons-vue'
import { getEquipmentPage } from '@/api/equipment'
import { getConsumablePage } from '@/api/consumable'
import type { EquipmentVO } from '@/api/equipment'
import type { ConsumableVO } from '@/api/consumable'

const router = useRouter()

/** 统计卡片数据 */
const stats = reactive({
  equipmentTotal: 0,
  consumableTotal: 0,
  lowStockCount: 0,
  maintenanceCount: 0,
})

/** 低库存耗材列表 */
const lowStockItems = ref<ConsumableVO[]>([])

/** 图表引用 */
const pieChartRef = ref()
const gaugeChartRef = ref()
let pieChart: echarts.ECharts
let gaugeChart: echarts.ECharts

/** 设备状态分布数据 */
const equipmentStatusData = ref([
  { name: '正常', value: 0, itemStyle: { color: '#67C23A' } },
  { name: '维修中', value: 0, itemStyle: { color: '#E6A23C' } },
  { name: '报废', value: 0, itemStyle: { color: '#F56C6C' } },
])

/** ========== 数据加载 ========== */

/**
 * 异步加载设备统计数据
 */
const fetchEquipmentStats = async () => {
  try {
    const res: any = await getEquipmentPage({ pageNum: 1, pageSize: 1 })
    stats.equipmentTotal = res.data.total || 0
    // 再次请求各状态数量（简化：取总数，业务中应有独立接口）
    // 状态数据通过图表接口单独拉取
  } catch {}
}

const fetchEquipmentChart = async () => {
  try {
    const res: any = await getEquipmentPage({ pageNum: 1, pageSize: 500 })
    const list: EquipmentVO[] = res.data.list || []
    const counts = { 0: 0, 1: 0, 2: 0 }
    list.forEach((e: EquipmentVO) => { if (counts[e.status] !== undefined) counts[e.status]++ })
    equipmentStatusData.value = [
      { name: '正常', value: counts[1], itemStyle: { color: '#67C23A' } },
      { name: '维修中', value: counts[0], itemStyle: { color: '#E6A23C' } },
      { name: '报废', value: counts[2], itemStyle: { color: '#F56C6C' } },
    ]
    stats.equipmentTotal = res.data.total || 0
    stats.maintenanceCount = counts[0]
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, textStyle: { fontSize: 13 } },
      series: [{
        type: 'pie',
        radius: ['42%', '72%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c}台', fontSize: 13 },
        emphasis: {
          itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,0.2)' },
          label: { fontSize: 15, fontWeight: 'bold' },
        },
        data: equipmentStatusData.value,
      }],
    })
  } catch {}
}

const fetchConsumableChart = async () => {
  try {
    const res: any = await getConsumablePage({ pageNum: 1, pageSize: 500 })
    const list: ConsumableVO[] = res.data.list || []
    stats.consumableTotal = res.data.total || 0

    // 库存预警：当前库存 <= 最低库存
    const lowStock = list.filter(c => c.currentStock <= c.minStockLevel)
    lowStockItems.value = lowStock
    stats.lowStockCount = lowStock.length

    // 仪表盘：正常/预警/危险 三档
    const normal = list.filter(c => c.currentStock > c.minStockLevel && c.currentStock < c.maxStockLevel).length
    const warning = lowStock.length
    const danger = list.filter(c => c.currentStock === 0).length

    gaugeChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      series: [{
        type: 'gauge',
        center: ['50%', '60%'],
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: list.length || 1,
        splitNumber: 4,
        axisLine: {
          lineStyle: { width: 20, color: [
            [0.3, '#67C23A'],
            [0.7, '#E6A23C'],
            [1, '#F56C6C'],
          ]},
        },
        pointer: { width: 5, length: '60%', itemStyle: { color: '#409EFF' } },
        axisTick: { distance: -20, length: 6 },
        splitLine: { distance: -22, length: 14 },
        axisLabel: { distance: -30, fontSize: 11, color: '#888' },
        detail: {
          valueAnimation: true,
          formatter: '{c} 项异常',
          fontSize: 16,
          fontWeight: 'bold',
          color: '#F56C6C',
          offsetCenter: [0, '40%'],
        },
        data: [{ value: warning, name: '库存预警' }],
      }],
    })
  } catch {}
}

/** ========== 辅助方法 ========== */
const stockPercentage = (row: ConsumableVO) => {
  const range = row.maxStockLevel - row.minStockLevel
  if (range <= 0) return 100
  return Math.min(100, Math.round(((row.currentStock - row.minStockLevel) / range) * 100))
}
const stockColor = (row: ConsumableVO) => {
  if (row.currentStock === 0) return '#F56C6C'
  if (row.currentStock <= row.minStockLevel) return '#E6A23C'
  return '#67C23A'
}
const stockClass = (row: ConsumableVO) => row.currentStock <= row.minStockLevel ? 'low-stock-text' : ''
const stockTagType = (row: ConsumableVO) => row.currentStock === 0 ? 'danger' : row.currentStock <= row.minStockLevel ? 'warning' : 'success'
const stockStatus = (row: ConsumableVO) => row.currentStock === 0 ? '耗尽' : row.currentStock <= row.minStockLevel ? '偏低' : '正常'

/** ========== 生命周期 ========== */
onMounted(() => {
  // 初始化图表实例
  pieChart = echarts.init(pieChartRef.value)
  gaugeChart = echarts.init(gaugeChartRef.value)

  // 加载数据
  fetchEquipmentStats()
  fetchEquipmentChart()
  fetchConsumableChart()

  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    pieChart.resize()
    gaugeChart.resize()
  })
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

.low-stock-text { color: #F56C6C; font-weight: bold; }
</style>
