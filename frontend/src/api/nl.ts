/**
 * 自然语言业务流 API
 * @description NL指令执行及因果预测接口
 */
import request from '@/utils/request'

/**
 * NL执行结果
 */
export interface NLExecuteResult {
  success: boolean
  message: string
}

/**
 * 因果检查结果
 */
export interface CausalCheckResult {
  causalCheckPerformed: boolean
  executionResult: string
  entityId: string | null
  impactedNodes: Record<string, number>
}

/**
 * 执行自然语言指令
 * @param input - 自然语言输入
 */
export const executeNL = (input: string): Promise<string> =>
  request.post<string>('/nl/execute', { input }) as unknown as Promise<string>

/**
 * 执行自然语言指令并进行因果影响预测
 * @param input - 自然语言输入
 */
export const executeWithCausalCheck = (input: string): Promise<CausalCheckResult> =>
  request.post<CausalCheckResult>('/nl/execute-with-causal-check', { input }) as unknown as Promise<CausalCheckResult>
