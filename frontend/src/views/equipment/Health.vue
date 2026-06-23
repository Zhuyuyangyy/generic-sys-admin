<template>
  <div class="health-page">
    <!-- Overview cards -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff);">
            <el-icon :size="28"><DataAnalysis /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.averageScore }}</div>
            <div class="stat-label">平均健康分</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #f3d19e);">
            <el-icon :size="28"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.warningCount }}</div>
            <div class="stat-label">风险设备</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #fab6b6);">
            <el-icon :size="28"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.criticalCount }}</div>
            <div class="stat-label">严重设备</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Grade distribution chart + Equipment health list -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>等级分布</span></template>
          <div ref="gradeChartRef" style="height: 300px;" />
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>设备健康评分</span>
              <el-button text size="small" @click="fetchData">刷新</el-button>
            </div>
          </template>
          <el-table :data="healthList" v-loading="loading" stripe size="small">
            <el-table-column prop="equipmentName" label="设备名称" min-width="140" />
            <el-table-column label="健康评分" width="200">
              <template #default="{ row }">
                <el-progress :percentage="row.score" :color="scoreColor(row.score)" :stroke-width="12" />
              </template>
            </el-table-column>
            <el-table-column label="等级" width="80">
              <template #default="{ row }">
                <el-tag :color="gradeColor(row.level)" style="color: #fff; border: none;" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastCheckedAt" label="最近检查" width="160">
              <template #default="{ row }">{{ row.lastCheckedAt?.replace('T', ' ').slice(0, 19) || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { DataAnalysis, Warning, CircleClose } from '@element-plus/icons-vue'
import { getAllHealthScores, getHealthOverview } from '@/api/health'
import type { AssetHealthScore, AssetHealthOverview } from '@/utils/types'

const loading = ref(false)
const healthList = ref<AssetHealthScore[]>([])
const overview = reactive<AssetHealthOverview>({ averageScore: 0, healthyCount: 0, warningCount: 0, criticalCount: 0, distribution: [] })
const gradeChartRef = ref()
let gradeChart: echarts.ECharts

const gradeColor = (level: string) => {
  const map: Record<string, string> = { A: '#67C23A', B: '#409EFF', C: '#E6A23C', D: '#F56C6C' }
  return map[level] || '#909399'
}
const scoreColor = (score: number) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#409EFF'
  if (score >= 40) return '#E6A23C'
  return '#F56C6C'
}

const fetchData = async () => {
  loading.value = true
  try {
    const [scores, ov] = await Promise.all([getAllHealthScores(), getHealthOverview()])
    healthList.value = scores
    Object.assign(overview, ov)
    renderChart()
  } catch {} finally { loading.value = false }
}

const renderChart = () => {
  const data = overview.distribution.map(d => ({
    name: d.level, value: d.count,
    itemStyle: { color: gradeColor(d.level) },
  }))
  gradeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}台' },
      data,
    }],
  })
}

onMounted(() => {
  gradeChart = echarts.init(gradeChartRef.value)
  fetchData()
  window.addEventListener('resize', () => { gradeChart.resize() })
})

onBeforeUnmount(() => { gradeChart?.dispose() })
</script>

<style scoped lang="scss">
.health-page { padding: 10px; }
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
</style>
