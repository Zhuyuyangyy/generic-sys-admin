<template>
  <div ref="chartRef" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  data: { name: string; value: number }[]
  height?: string
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts

const buildOption = () => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: '#8c8c8c' } },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    roseType: 'area',
    itemStyle: { borderRadius: 5 },
    data: props.data
  }]
})

onMounted(() => {
  chart = echarts.init(chartRef.value!)
  chart.setOption(buildOption())
  window.addEventListener('resize', () => chart.resize())
})

watch(() => props.data, () => chart?.setOption(buildOption()), { deep: true })
</script>
