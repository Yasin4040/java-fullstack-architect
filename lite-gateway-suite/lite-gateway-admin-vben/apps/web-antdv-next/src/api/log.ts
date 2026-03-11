import { requestClient } from './request'
import type { 
  PageBody, 
  LogQuery, 
  GatewayLog, 
  LogStatistics, 
  RouteRank, 
  StatusDistribution, 
  ResponseTimeTrend 
} from '@/types/api'

// 日志管理 API
export const logApi = {
  // 分页查询网关日志
  getLogPage: (params: LogQuery) => {
    return requestClient.get<PageBody<GatewayLog>>('/gateway/log/page', { params })
  },

  // 获取日志统计信息
  getLogStatistics: () => {
    return requestClient.get<LogStatistics>('/gateway/log/statistics')
  },

  // 获取路由访问排行
  getRouteRanking: () => {
    return requestClient.get<RouteRank[]>('/gateway/log/ranking')
  },

  // 获取状态码分布
  getStatusDistribution: () => {
    return requestClient.get<StatusDistribution[]>('/gateway/log/status-distribution')
  },

  // 获取响应时间趋势
  getResponseTimeTrend: (hours?: number) => {
    return requestClient.get<ResponseTimeTrend[]>('/gateway/log/response-time-trend', {
      params: { hours: hours || 24 }
    })
  },

  // 清理日志
  cleanLogs: (days?: number) => {
    return requestClient.delete<void>('/gateway/log/clean', {
      params: { days: days || 7 }
    })
  }
}

export default logApi
