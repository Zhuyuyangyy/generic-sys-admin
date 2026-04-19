<template>
  <div ref="chartRef" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  data: any
  height?: string
}>()

const chartRef = ref<HTMLElement>()

onMounted(() => {
  const chart = echarts.init(chartRef.value!, 'transparent')

  // 使用 geo3D 或者 globe 效果
  // ⚠️ 如需接入医疗器械/牙科机器人3D模型：
  // 1. 将 .glb 模型放入 frontend/public/models/
  // 2. 使用 GLTFLoader 加载：loader.load('/models/your-model.glb', ...)
  const option = {
    backgroundColor: 'transparent',
    globe: {
      baseTexture: '/geo/world.jpg',      // 需放置 world.jpg 至 public/geo/
      heightTexture: '/geo/world.jpg',
      displacementScale: 0.04,
      shading: 'realistic',
      realisticMaterial: { roughness: 0.8 },
      ambientLight: 0.3,
      directionalLight: 0.5,
      starIntensity: 0.8,
    },
    series: [{
      type: 'lines3D',
      coordinateSystem: 'globe',
      effect: { show: true, trailWidth: 2, trailOpacity: 0.5, trailLength: 0.2 },
      lineStyle: { width: 1, color: '#00f2fe', opacity: 0.8 },
      data: [],  // 可传入航线/连接线数据
    }]
  }

  chart.setOption(option)

  // 如果 world.jpg 不存在，则显示简单地球仪演示
  chart.on('renderfailed', () => {
    chart.clear()
    chart.setOption({
      backgroundColor: 'transparent',
      globe: {
        baseTexture: undefined,
        shading: 'lambert',
        ambientLight: 0.5,
        globeRadius: 80,
        globeOuterRadius: 90,
      },
      series: [{
        type: 'bar3D',
        coordinateSystem: 'globe',
        data: [],
      }]
    })
  })
})
</script>
