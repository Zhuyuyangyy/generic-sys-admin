/**
 * 自然语言交互模块 API
 * @description 自然语言解析、因果校验、试运行、确认执行等接口封装
 */
import request from '@/utils/request'
import type { DryRunResult, ExecuteResult } from '@/utils/types'

/**
 * 带因果校验的自然语言执行
 * @param input - 自然语言输入
 */
export const executeWithCausalCheck = (input: string): Promise<ExecuteResult> =>
  request.post<ExecuteResult>('/nl/execute-with-causal-check', { input }) as unknown as Promise<ExecuteResult>

/**
 * 解析自然语言为结构化意图
 * @param input - 自然语言输入
 */
export const parseNL = (input: string) =>
  request.post('/nl/parse', { input })

/**
 * 获取测试命令列表
 */
export const getTestCommands = () =>
  request.get('/nl/test-commands')

/**
 * 试运行自然语言命令（不实际执行）
 * @param input - 自然语言输入
 */
export const dryRun = (input: string): Promise<DryRunResult> =>
  request.post<DryRunResult>('/nl/dry-run', { input }) as unknown as Promise<DryRunResult>

/**
 * 确认执行自然语言命令
 * @param input - 自然语言输入
 * @param confirmationId - 确认 ID（由试运行返回）
 */
export const executeConfirmed = (input: string, confirmationId: string): Promise<ExecuteResult> =>
  request.post<ExecuteResult>('/nl/execute-confirmed', { input, confirmationId }) as unknown as Promise<ExecuteResult>
