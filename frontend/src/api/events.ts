/**
 * 事件订阅模块 API
 * @description 系统事件最近记录与订阅信息接口封装
 */
import request from '@/utils/request'

/**
 * 获取最近事件列表
 */
export const getRecentEvents = () =>
  request.get('/events/recent')

/**
 * 获取事件订阅信息
 */
export const getSubscribeInfo = () =>
  request.get('/events/subscribe-info')
