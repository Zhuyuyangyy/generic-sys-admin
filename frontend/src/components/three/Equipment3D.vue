<template>
  <el-card style="margin-top: 20px;">
    <template #header>
      <div class="card-header">
        <span>3D 设备模型演示</span>
        <el-button type="primary" size="small" @click="generateVideo" :loading="videoLoading">
          <el-icon><VideoCamera /></el-icon>
          AI 生成视频演示
        </el-button>
      </div>
    </template>
    <div ref="threeContainer" class="three-canvas" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { VideoCamera } from '@element-plus/icons-vue'

const threeContainer = ref()
const videoLoading = ref(false)
let animationId: number
let renderer: THREE.WebGLRenderer
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let cube: THREE.Mesh

const initThree = () => {
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf0f2f5)

  camera = new THREE.PerspectiveCamera(75, threeContainer.value.clientWidth / threeContainer.value.clientHeight, 0.1, 1000)
  camera.position.z = 3

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(threeContainer.value.clientWidth, threeContainer.value.clientHeight)
  threeContainer.value.appendChild(renderer.domElement)

  const geometry = new THREE.BoxGeometry(1, 1, 1)
  const material = new THREE.MeshStandardMaterial({ color: 0x409EFF, metalness: 0.5, roughness: 0.3 })
  cube = new THREE.Mesh(geometry, material)
  scene.add(cube)

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)
  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(2, 2, 2)
  scene.add(directionalLight)

  const animate = () => {
    animationId = requestAnimationFrame(animate)
    cube.rotation.x += 0.01
    cube.rotation.y += 0.01
    renderer.render(scene, camera)
  }
  animate()
}

const generateVideo = () => {
  videoLoading.value = true
  setTimeout(() => {
    videoLoading.value = false
  }, 3000)
}

onMounted(() => {
  initThree()
  window.addEventListener('resize', () => {
    if (camera && renderer) {
      camera.aspect = threeContainer.value.clientWidth / threeContainer.value.clientHeight
      camera.updateProjectionMatrix()
      renderer.setSize(threeContainer.value.clientWidth, threeContainer.value.clientHeight)
    }
  })
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  renderer?.dispose()
})
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.three-canvas {
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e8e8e8;
}
</style>
