import { requestClient } from './request'
import type { 
  PageBody, 
  RouteVO, 
  RouteDTO,
  RouteQuery,
  Instance,
  InstanceDTO,
  InstanceQuery,
  InterfaceDTO 
} from '@/types/api'

// 路由管理 API - 与后端 GatewayRouteController 保持一致
export const routeApi = {
  // 分页查询路由列表 - GET /gateway/route/page
  getRoutePage: (params: RouteQuery) => {
    return requestClient.get<PageBody<RouteVO>>('/gateway/route/page', { params })
  },

  // 查询路由列表 - GET /gateway/route/list
  getRouteList: (params?: RouteQuery) => {
    return requestClient.get<RouteVO[]>('/gateway/route/list', { params })
  },

  // 根据ID获取路由详情 - GET /gateway/route/{id}
  getRouteById: (id: number | string) => {
    return requestClient.get<RouteDTO>(`/gateway/route/${id}`)
  },

  // 添加路由 - POST /gateway/route
  addRoute: (data: RouteDTO) => {
    return requestClient.post<void>('/gateway/route', data)
  },

  // 更新路由 - PUT /gateway/route/{id}
  updateRoute: (id: number | string, data: RouteDTO) => {
    return requestClient.put<void>(`/gateway/route/${id}`, data)
  },

  // 删除路由 - DELETE /gateway/route/{id}
  deleteRoute: (id: number | string) => {
    return requestClient.delete<void>(`/gateway/route/${id}`)
  },

  // 修改路由状态 - PATCH /gateway/route/{id}/status
  updateRouteStatus: (id: number | string, status: string) => {
    return requestClient.patch<void>(`/gateway/route/${id}/status`, null, {
      params: { status }
    })
  },

  // 刷新配置（重新加载网关路由配置）- POST /gateway/route/reload
  reloadConfig: () => {
    return requestClient.post<void>('/gateway/route/reload')
  },

  // ==================== 实例管理接口 ====================

  // 获取服务所有实例（从 Nacos 获取）- GET /gateway/route/instances
  getInstances: (serviceName: string) => {
    return requestClient.get<Instance[]>('/gateway/route/instances', {
      params: { serviceName }
    })
  },

  // 分页获取服务实例 - GET /gateway/route/instances/page
  getInstancesPage: (params: InstanceQuery) => {
    return requestClient.get<PageBody<Instance>>('/gateway/route/instances/page', { params })
  },

  // 更新实例权重 - POST /gateway/route/instances/weight
  updateInstanceWeight: (data: InstanceDTO) => {
    return requestClient.post<void>('/gateway/route/instances/weight', data)
  },

  // 更新实例启用状态（上下线）- POST /gateway/route/instances/enabled
  updateInstanceEnabled: (data: InstanceDTO) => {
    return requestClient.post<void>('/gateway/route/instances/enabled', data)
  },

  // ==================== 接口管理接口 ====================

  // 获取服务所有接口（从 Swagger API Docs 获取）- GET /gateway/route/{id}/interfaces
  getInterfaces: (id: number | string) => {
    return requestClient.get<InterfaceDTO[]>(`/gateway/route/${id}/interfaces`)
  }
}

export default routeApi
