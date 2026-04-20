/**
 * AI 服务模块 API
 * @description MiniMax 多模态 AI 服务封装：语音合成、图像生成、视频生成
 */
import request from '@/utils/request'

/**
 * TTS 语音合成响应
 */
export interface TTSResponse {
  audioUrl: string
  audioFileId: string
  model: string
}

/**
 * 图像生成响应
 */
export interface ImageResponse {
  imageUrl: string
  revisedPrompt: string
  model: string
}

/**
 * 视频生成响应
 */
export interface VideoResponse {
  jobId: string
  status: string
  videoUrl?: string
  model: string
}

/**
 * 语音合成
 * @param text - 待合成文本
 * @param voice - 音色（默认 male-qn-qingse）
 */
export const textToSpeech = (text: string, voice = 'male-qn-qingse'): Promise<TTSResponse> =>
  request.post<TTSResponse>(`/ai/tts?text=${encodeURIComponent(text)}&voice=${voice}`) as unknown as Promise<TTSResponse>

/**
 * 图像生成
 * @param prompt - 图像描述
 * @param aspectRatio - 尺寸比例（1:1 / 16:9 / 9:16 / 3:4 / 4:3）
 */
export const generateImage = (prompt: string, aspectRatio = '1:1'): Promise<ImageResponse> =>
  request.post<ImageResponse>(`/ai/image?prompt=${encodeURIComponent(prompt)}&aspectRatio=${aspectRatio}`) as unknown as Promise<ImageResponse>

/**
 * 提交视频生成任务
 * @param prompt - 视频描述
 * @param duration - 时长（5/10 秒）
 */
export const generateVideo = (prompt: string, duration = 5): Promise<VideoResponse> =>
  request.post<VideoResponse>(`/ai/video/generate?prompt=${encodeURIComponent(prompt)}&duration=${duration}`) as unknown as Promise<VideoResponse>

/**
 * 查询视频生成状态
 * @param jobId - 任务 ID
 */
export const getVideoStatus = (jobId: string): Promise<VideoResponse> =>
  request.get<VideoResponse>(`/ai/video/status/${jobId}`) as unknown as Promise<VideoResponse>

/**
 * 获取可用 AI 模型列表
 */
export const getAIModels = () =>
  request.get<{ tts: string[]; image: string[]; video: string[] }>('/ai/models') as unknown as Promise<{ tts: string[]; image: string[]; video: string[] }>
