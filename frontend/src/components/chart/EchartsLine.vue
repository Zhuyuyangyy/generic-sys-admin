<template>
  <div ref="chartRef" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  data: { xAxis: string[]; series: number[] }
  height?: string
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts

const buildOption = () => ({
  tooltip: { trigger: 'axis' },
  grid: { top: 20, right: 20, bottom: 30, left: 50 },
  xAxis: {
    type: 'category',
    data: props.data.xAxis,
    axisLine: { lineStyle: { color: '#333' } },
    axisLabel: { color: '#8c8c8c' }
  },
  yAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: '#333' } },
    axisLabel: { color: '#8c8c8c' },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
  },
  series: [{
    data: props.data.series,
    type: 'line',
    smooth: true,
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(64,158,255,0.4)' },
        { offset: 1, color: 'rgba(64,158,255,0)' }
      ])
    },
    lineStyle: { color: '#409EFF' },
    itemStyle: { color: '#409EFF' }
  }]
})

onMounted(() => {
  chart = echarts.init(chartRef.value!)
  chart.setOption(buildOption())
  window.addEventListener('resize', () => chart.resize())
})

watch(() => props.data, () => chart?.setOption(buildOption()), { deep: true })
</script>
