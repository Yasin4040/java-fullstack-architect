import { requestClient } from './request'
import type { 
  PageBody, 
  RateLimitRule, 
  RateLimitQuery 
} from '@/types/api'

// 限流规则管理 API
export const rateLimitApi = {
  // 分页查询限流规则列表
  getRateLimitPage: (params: RateLimitQuery) => {
    return requestClient.get<PageBody<RateLimitRule>>('/gateway/rate-limit/page', { params })
  },

  // 获取所有限流规则
  getAllRateLimits: () => {
    return requestClient.get<RateLimitRule[]>('/gateway/rate-limit/list')
  },

  // 根据ID获取限流规则
  getRateLimitById: (id: number) => {
    return requestClient.get<RateLimitRule>(`/gateway/rate-limit/${id}`)
  },

  // 添加限流规则
  addRateLimit: (data: RateLimitRule) => {
    return requestClient.post<void>('/gateway/rate-limit', data)
  },

  // 更新限流规则
  updateRateLimit: (id: number, data: RateLimitRule) => {
    return requestClient.put<void>(`/gateway/rate-limit/${id}`, data)
  },

  // 删除限流规则
  deleteRateLimit: (id: number) => {
    return requestClient.delete<void>(`/gateway/rate-limit/${id}`)
  },

  // 修改限流规则状态
  updateRateLimitStatus: (id: number, status: number) => {
    return requestClient.patch<void>(`/gateway/rate-limit/${id}/status`, null, {
      params: { status }
    })
  },

  // 获取路由的限流规则
  getRateLimitsByRouteId: (routeId: string) => {
    return requestClient.get<RateLimitRule[]>('/gateway/rate-limit/route', {
      params: { routeId }
    })
  }
}

export default rateLimitApi
