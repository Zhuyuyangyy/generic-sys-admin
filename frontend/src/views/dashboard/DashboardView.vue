<template>
  <div class="dashboard" :style="{ background: themeConfig.background }">
    <!-- 顶部标题栏 -->
    <div class="dashboard-header">
      <h1 class="title">{{ title }}</h1>
      <div class="header-right">
        <span class="time">{{ currentTime }}</span>
        <el-button @click="toggleTheme" size="small" circle icon="Sunny" />
      </div>
    </div>

    <!-- 中部3列内容 -->
    <div class="dashboard-body">
      <!-- 左列 -->
      <div class="panel left">
        <div class="panel-card">
          <div class="panel-title">📊 今日数据概览</div>
          <EchartsLine :data="todayData" height="220px" />
        </div>
        <div class="panel-card">
          <div class="panel-title">📈 趋势分析</div>
          <EchartsBar :data="trendData" height="200px" />
        </div>
      </div>

      <!-- 中列（主指标） -->
      <div class="panel center">
        <div class="main-indicator">
          <div class="indicator-item">
            <div class="indicator-value text-primary">{{ stats.totalUsers }}</div>
            <div class="indicator-label">总用户数</div>
          </div>
          <div class="indicator-item">
            <div class="indicator-value text-success">{{ stats.activeUsers }}</div>
            <div class="indicator-label">活跃用户</div>
          </div>
          <div class="indicator-item">
            <div class="indicator-value text-warning">{{ stats.totalOrders }}</div>
            <div class="indicator-label">总订单数</div>
          </div>
          <div class="indicator-item">
            <div class="indicator-value text-danger">{{ stats.totalAmount }}</div>
            <div class="indicator-label">总金额(万)</div>
          </div>
        </div>
        <div class="center-chart">
          <EchartsPie :data="pieData" height="320px" />
        </div>
      </div>

      <!-- 右列 -->
      <div class="panel right">
        <div class="panel-card">
          <div class="panel-title">🌐 实时地图</div>
          <Echarts3D :data="mapData" height="240px" />
        </div>
        <div class="panel-card">
          <div class="panel-title">🔥 热力榜单</div>
          <el-table :data="rankList" size="small" class="rank-table">
            <el-table-column prop="rank" label="排名" width="50" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="value" label="数值" align="right" />
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import EchartsLine from '@/components/chart/EchartsLine.vue'
import EchartsBar from '@/components/chart/EchartsBar.vue'
import EchartsPie from '@/components/chart/EchartsPie.vue'
import Echarts3D from '@/components/chart/Echarts3D.vue'

const title = '数据驾驶舱'
const currentTime = ref('')
const themeConfig = ref({ background: 'linear-gradient(135deg, #0a0f1c 0%, #1a1f3c 100%)' })

const todayData = ref({ xAxis: [], series: [] })
const trendData = ref({ xAxis: [], series: [] })
const pieData = ref([])
const mapData = ref([])
const rankList = ref([])
const stats = ref({ totalUsers: 0, activeUsers: 0, totalOrders: 0, totalAmount: 0 })

let timer: number

const toggleTheme = () => {
  const isDark = themeConfig.value.background.includes('#0a0f1c')
  themeConfig.value.background = isDark
    ? 'linear-gradient(135deg, #ffffff 0%, #f0f2f5 100%)'
    : 'linear-gradient(135deg, #0a0f1c 0%, #1a1f3c 100%)'
}

const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN')
}

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  color: #fff;
  padding: 16px;
}
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  margin-bottom: 16px;
}
.title {
  font-size: 28px;
  font-weight: bold;
  background: linear-gradient(90deg, #00f2fe, #00d4ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.dashboard-body {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  gap: 16px;
}
.panel { display: flex; flex-direction: column; gap: 16px; }
.panel-card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  padding: 16px;
}
.panel-title { font-size: 14px; margin-bottom: 12px; color: #8c8c8c; }
.main-indicator {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.indicator-item { text-align: center; padding: 20px; }
.indicator-value { font-size: 36px; font-weight: bold; }
.text-primary { color: #409EFF; }
.text-success { color: #67C23A; }
.text-warning { color: #E6A23C; }
.text-danger { color: #F56C6C; }
.indicator-label { font-size: 12px; color: #8c8c8c; margin-top: 8px; }
.rank-table { background: transparent; color: #fff; }
</style>
