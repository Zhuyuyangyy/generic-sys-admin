import request from '@/api/request'

/**
 * TTS 语音播报 API
 */
export const ttsApi = {
  /**
   * 文本转语音
   * @param text 文本内容
   * @param voiceId 音色ID
   */
  synthesize: (text: string, voiceId?: string) =>
    request.post<{ data: string }>('/voice/synthesize', {
      text,
      voiceId: voiceId || 'female-tianmei',
      speed: 1.0,
      volume: 50,
      pitch: 1.0,
    }),
}

/**
 * TTS 语音播报 Hook
 */
import { ref } from 'vue'

export function useTts() {
  const isPlaying = ref(false)
  const audioInstance = ref<HTMLAudioElement | null>(null)

  const play = async (text: string, voiceId?: string) => {
    if (isPlaying.value) return

    isPlaying.value = true
    try {
      const res = await ttsApi.synthesize(text, voiceId)
      const audioUrl = res.data

      audioInstance.value = new Audio(audioUrl)
      audioInstance.value.onended = () => { isPlaying.value = false }
      audioInstance.value.onerror = () => { isPlaying.value = false }
      await audioInstance.value.play()
    } catch (err) {
      isPlaying.value = false
      console.error('TTS播放失败:', err)
    }
  }

  const stop = () => {
    if (audioInstance.value) {
      audioInstance.value.pause()
      audioInstance.value.currentTime = 0
      isPlaying.value = false
    }
  }

  return { play, stop, isPlaying }
}
