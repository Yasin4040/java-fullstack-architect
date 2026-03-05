import request from '@/utils/request'
import type { 
  PageBody, 
  GatewayLog, 
  LogQuery,
  LogStatistics,
  RouteRank,
  StatusDistribution,
  ResponseTimeTrend
} from '@/types/api'

// 日志管理 API
export const logApi = {
  // 分页查询访问日志
  getLogPage: (params: LogQuery) => {
    return request.get<any, PageBody<GatewayLog>>('/gateway/log/page', { params })
  },

  // 获取日志详情
  getLogById: (id: string) => {
    return request.get<any, GatewayLog>(`/gateway/log/${id}`)
  },

  // 获取日志统计
  getLogStatistics: (params: { startTime?: string; endTime?: string }) => {
    return request.get<any, LogStatistics>('/gateway/log/statistics', { params })
  },

  // 获取路由访问排行
  getRouteRank: (params: { startTime?: string; endTime?: string; top?: number }) => {
    return request.get<any, RouteRank[]>('/gateway/log/route-rank', { params })
  },

  // 获取状态码分布
  getStatusDistribution: (params: { startTime?: string; endTime?: string }) => {
    return request.get<any, StatusDistribution[]>('/gateway/log/status-distribution', { params })
  },

  // 获取响应时间趋势
  getResponseTimeTrend: (params: { startTime?: string; endTime?: string; interval?: string }) => {
    return request.get<any, ResponseTimeTrend[]>('/gateway/log/response-time-trend', { params })
  }
}

export default logApi
