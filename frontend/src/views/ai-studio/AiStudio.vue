<template>
  <div class="ai-studio">
    <el-page-header title="智能演播厅" @back="router.back()" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- TTS 语音合成 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><Microphone /></el-icon> 语音合成
              </span>
              <el-tag type="info" size="small">speech-01</el-tag>
            </div>
          </template>

          <el-form label-position="top">
            <el-form-item label="音色">
              <el-select v-model="ttsVoice" style="width: 100%">
                <el-option label="清澈男声 (male-qn-qingse)" value="male-qn-qingse" />
                <el-option label="温柔女声 (female-tianmei)" value="female-tianmei" />
                <el-option label="活力少女 (female-yunyang)" value="female-yunyang" />
                <el-option label="沉稳男声 (male-yunyang)" value="male-yunyang" />
              </el-select>
            </el-form-item>
            <el-form-item label="合成文本">
              <el-input
                v-model="ttsText"
                type="textarea"
                :rows="4"
                placeholder="请输入需要合成语音的文本..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%;" :loading="ttsLoading" @click="handleTTS">
                <el-icon v-if="!ttsLoading"><Microphone /></el-icon>
                生成语音
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 音频播放 -->
          <audio v-if="ttsAudioUrl" ref="ttsAudioRef" :src="ttsAudioUrl" controls style="width: 100%; margin-top: 12px;" />
        </el-card>
      </el-col>

      <!-- 图像生成 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><Picture /></el-icon> 图像生成
              </span>
              <el-tag type="info" size="small">image-01</el-tag>
            </div>
          </template>

          <el-form label-position="top">
            <el-form-item label="尺寸比例">
              <el-radio-group v-model="imageAspect" style="width: 100%;">
                <el-radio-button value="1:1">1:1 方图</el-radio-button>
                <el-radio-button value="16:9">16:9 横图</el-radio-button>
                <el-radio-button value="9:16">9:16 竖图</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="图像描述（英文效果更佳）">
              <el-input
                v-model="imagePrompt"
                type="textarea"
                :rows="4"
                placeholder="A futuristic robot working in a modern laboratory, cinematic lighting..."
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%;" :loading="imageLoading" @click="handleImage">
                <el-icon v-if="!imageLoading"><Picture /></el-icon>
                生成图像
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 图像展示 -->
          <div v-if="imageUrl" class="image-preview">
            <el-image :src="imageUrl" fit="contain" :preview-src-list="[imageUrl]" style="width: 100%; height: 220px;" />
            <div v-if="imageRevisedPrompt" class="prompt-hint">
              <span class="prompt-label">优化描述：</span>{{ imageRevisedPrompt }}
            </div>
          </div>
          <el-empty v-else description="生成的图像将在此展示" :image-size="80" style="height: 220px;" />
        </el-card>
      </el-col>

      <!-- 视频生成 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><VideoCamera /></el-icon> 视频生成
              </span>
              <el-tag type="info" size="small">video-01</el-tag>
            </div>
          </template>

          <el-form label-position="top">
            <el-form-item label="视频时长">
              <el-radio-group v-model="videoDuration">
                <el-radio-button :value="5">5 秒</el-radio-button>
                <el-radio-button :value="10">10 秒</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="视频描述">
              <el-input
                v-model="videoPrompt"
                type="textarea"
                :rows="4"
                placeholder="A robot arm assembling electronic components in a factory..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%;" :loading="videoLoading" @click="handleVideo">
                <el-icon v-if="!videoLoading"><VideoCamera /></el-icon>
                生成视频
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 视频状态 / 播放 -->
          <div v-if="videoUrl" class="video-preview">
            <video :src="videoUrl" controls style="width: 100%; border-radius: 6px;" />
          </div>
          <div v-else-if="videoJobId" class="video-status">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>视频生成中（JobId: {{ videoJobId }}）...</span>
            <el-button size="small" @click="pollVideoStatus" :loading="videoPollLoading">
              刷新状态
            </el-button>
          </div>
          <el-empty v-else description="生成的视频将在此播放" :image-size="80" style="height: 160px;" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 交互提示 -->
    <el-alert type="info" :closable="false" style="margin-top: 20px;">
      <template #title>
        <span>💡 当前为 <strong>MiniMax API 演示模式</strong>，所有 AI 调用直接对接后端 /api/ai 接口。
          视频生成为异步任务，提交后请等待数秒后点击「刷新状态」查看结果。</span>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
/**
 * 智能演播厅视图
 * @description MiniMax 多模态 AI 功能演示页面，集成语音合成、图像生成、视频生成三大能力
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Microphone, Picture, VideoCamera, Loading } from '@element-plus/icons-vue'
import { textToSpeech, generateImage, generateVideo, getVideoStatus } from '@/api/ai'

const router = useRouter()

// ========== TTS ==========
const ttsVoice = ref('male-qn-qingse')
const ttsText = ref('欢迎使用通用管理系统智能演播厅，这里可以体验语音合成、图像生成和视频生成等多种 AI 能力。')
const ttsLoading = ref(false)
const ttsAudioUrl = ref('')
const ttsAudioRef = ref()

const handleTTS = async () => {
  if (!ttsText.value.trim()) {
    ElMessage.warning('请输入合成文本')
    return
  }
  ttsLoading.value = true
  ttsAudioUrl.value = ''
  try {
    const res: any = await textToSpeech(ttsText.value, ttsVoice.value)
    if (res.data?.audioUrl) {
      ttsAudioUrl.value = res.data.audioUrl
      ElMessage.success('语音合成成功')
      // 自动播放
      setTimeout(() => ttsAudioRef.value?.play(), 100)
    } else {
      ElMessage.warning('音频地址为空，请检查 API Key 配置')
    }
  } catch {
    // 错误由响应拦截器处理
  } finally {
    ttsLoading.value = false
  }
}

// ========== Image ==========
const imageAspect = ref('1:1')
const imagePrompt = ref('A sleek humanoid robot standing in a futuristic laboratory with holographic displays and blue ambient lighting.')
const imageLoading = ref(false)
const imageUrl = ref('')
const imageRevisedPrompt = ref('')

const handleImage = async () => {
  if (!imagePrompt.value.trim()) {
    ElMessage.warning('请输入图像描述')
    return
  }
  imageLoading.value = true
  imageUrl.value = ''
  imageRevisedPrompt.value = ''
  try {
    const res: any = await generateImage(imagePrompt.value, imageAspect.value)
    if (res.data?.imageUrl) {
      imageUrl.value = res.data.imageUrl
      imageRevisedPrompt.value = res.data.revisedPrompt || ''
      ElMessage.success('图像生成成功')
    } else {
      ElMessage.warning('图像地址为空，请检查 API Key 配置')
    }
  } catch {
    // 错误由响应拦截器处理
  } finally {
    imageLoading.value = false
  }
}

// ========== Video ==========
const videoDuration = ref(5)
const videoPrompt = ref('A robotic arm carefully assembling circuit boards on a high-tech production line.')
const videoLoading = ref(false)
const videoPollLoading = ref(false)
const videoJobId = ref('')
const videoUrl = ref('')

let pollTimer: ReturnType<typeof setTimeout> | null = null

const handleVideo = async () => {
  if (!videoPrompt.value.trim()) {
    ElMessage.warning('请输入视频描述')
    return
  }
  videoLoading.value = true
  videoJobId.value = ''
  videoUrl.value = ''
  if (pollTimer) { clearTimeout(pollTimer); pollTimer = null }

  try {
    const res: any = await generateVideo(videoPrompt.value, videoDuration.value)
    if (res.data?.jobId) {
      videoJobId.value = res.data.jobId
      ElMessage.success('视频生成任务已提交，请稍后刷新状态')
      // 自动轮询
      pollTimer = setTimeout(pollVideoStatus, 5000)
    }
  } catch {
    // 错误由响应拦截器处理
  } finally {
    videoLoading.value = false
  }
}

const pollVideoStatus = async () => {
  if (!videoJobId.value) return
  videoPollLoading.value = true
  try {
    const res: any = await getVideoStatus(videoJobId.value)
    const status = res.data?.status
    if (status === 'success' && res.data?.videoUrl) {
      videoUrl.value = res.data.videoUrl
      videoJobId.value = ''
      ElMessage.success('视频生成完成！')
      if (pollTimer) { clearTimeout(pollTimer); pollTimer = null }
    } else if (status === 'fail') {
      ElMessage.error('视频生成失败')
      videoJobId.value = ''
    } else {
      ElMessage.info(`当前状态：${status || 'processing'}，继续等待...`)
      pollTimer = setTimeout(pollVideoStatus, 8000)
    }
  } catch {
    ElMessage.error('查询视频状态失败')
  } finally {
    videoPollLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.ai-studio {
  padding: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-preview {
  margin-top: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  .prompt-hint {
    padding: 8px 12px;
    font-size: 12px;
    color: #888;
    background: #f9f9f9;
    .prompt-label { font-weight: bold; color: #409EFF; }
  }
}

.video-status {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background: #f0f9ff;
  border-radius: 6px;
  color: #409EFF;
  font-size: 13px;
}
</style>
