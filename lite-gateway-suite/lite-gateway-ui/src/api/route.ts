import request from '@/utils/request'
import type { 
  PageResult, 
  Route, 
  RouteQuery,
  Instance,
  InterfaceInfo 
} from '@/types/api'

// 路由管理 API
export const routeApi = {
  // 分页查询路由列表
  getRoutePage: (params: RouteQuery) => {
    return request.get<any, PageResult<Route>>('/gateway/route/page', { params })
  },

  // 获取所有路由
  getAllRoutes: () => {
    return request.get<any, Route[]>('/gateway/route/list')
  },

  // 根据ID获取路由
  getRouteById: (id: number) => {
    return request.get<any, Route>(`/gateway/route/${id}`)
  },

  // 添加路由
  addRoute: (data: Route) => {
    return request.post<any, void>('/gateway/route', data)
  },

  // 更新路由
  updateRoute: (id: number, data: Route) => {
    return request.put<any, void>(`/gateway/route/${id}`, data)
  },

  // 删除路由
  deleteRoute: (id: number) => {
    return request.delete<any, void>(`/gateway/route/${id}`)
  },

  // 修改路由状态
  updateRouteStatus: (id: number, status: number) => {
    return request.patch<any, void>(`/gateway/route/${id}/status`, null, {
      params: { status }
    })
  },

  // 刷新路由配置
  reloadConfig: () => {
    return request.post<any, void>('/gateway/route/reload')
  },

  // 获取服务实例列表
  getInstances: (serviceName: string) => {
    return request.get<any, Instance[]>('/gateway/route/instances', {
      params: { serviceName }
    })
  },

  // 获取服务接口列表
  getInterfaces: (serviceName: string) => {
    return request.get<any, InterfaceInfo[]>('/gateway/route/interfaces', {
      params: { serviceName }
    })
  }
}

export default routeApi
