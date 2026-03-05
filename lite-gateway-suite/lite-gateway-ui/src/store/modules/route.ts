import { defineStore } from 'pinia'
import { ref } from 'vue'
import { routeApi } from '@/api/route'
import type { 
  RouteVO, 
  RouteDTO, 
  RouteQuery, 
  Instance, 
  InstanceDTO,
  InstanceQuery,
  InterfaceDTO,
  PageBody 
} from '@/types/api'

export const useRouteStore = defineStore('route', () => {
  // State
  const routeList = ref<RouteVO[]>([])
  const currentRoute = ref<RouteDTO | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const pages = ref(0)
  const instances = ref<Instance[]>([])
  const instanceTotal = ref(0)
  const instancePages = ref(0)
  const interfaces = ref<InterfaceDTO[]>([])

  // Actions
  
  // 分页查询路由列表
  const fetchRoutePage = async (params: RouteQuery) => {
    loading.value = true
    try {
      const res: PageBody<RouteVO> = await routeApi.getRoutePage(params)
      routeList.value = res.list
      total.value = res.total
      pages.value = res.pages
      return res
    } finally {
      loading.value = false
    }
  }

  // 查询路由列表（不分页）
  const fetchRouteList = async (params?: RouteQuery) => {
    loading.value = true
    try {
      const res: RouteVO[] = await routeApi.getRouteList(params)
      routeList.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  // 根据ID获取路由详情
  const fetchRouteById = async (id: number | string) => {
    loading.value = true
    try {
      const res: RouteDTO = await routeApi.getRouteById(id)
      currentRoute.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  // 添加路由
  const addRoute = async (data: RouteDTO) => {
    await routeApi.addRoute(data)
  }

  // 更新路由
  const updateRoute = async (id: number | string, data: RouteDTO) => {
    await routeApi.updateRoute(id, data)
  }

  // 删除路由
  const deleteRoute = async (id: number | string) => {
    await routeApi.deleteRoute(id)
  }

  // 修改路由状态
  const updateRouteStatus = async (id: number | string, status: string) => {
    await routeApi.updateRouteStatus(id, status)
  }

  // 刷新配置
  const reloadConfig = async () => {
    await routeApi.reloadConfig()
  }

  // ==================== 实例管理 ====================

  // 获取服务所有实例
  const fetchInstances = async (serviceName: string) => {
    const res: Instance[] = await routeApi.getInstances(serviceName)
    instances.value = res
    return res
  }

  // 分页获取服务实例
  const fetchInstancesPage = async (params: InstanceQuery) => {
    const res: PageBody<Instance> = await routeApi.getInstancesPage(params)
    instances.value = res.list
    instanceTotal.value = res.total
    instancePages.value = res.pages
    return res
  }

  // 更新实例权重
  const updateInstanceWeight = async (data: InstanceDTO) => {
    await routeApi.updateInstanceWeight(data)
  }

  // 更新实例启用状态
  const updateInstanceEnabled = async (data: InstanceDTO) => {
    await routeApi.updateInstanceEnabled(data)
  }

  // ==================== 接口管理 ====================

  // 获取服务所有接口
  const fetchInterfaces = async (id: number | string) => {
    const res: InterfaceDTO[] = await routeApi.getInterfaces(id)
    interfaces.value = res
    return res
  }

  return {
    // State
    routeList,
    currentRoute,
    loading,
    total,
    pages,
    instances,
    instanceTotal,
    instancePages,
    interfaces,
    // Actions
    fetchRoutePage,
    fetchRouteList,
    fetchRouteById,
    addRoute,
    updateRoute,
    deleteRoute,
    updateRouteStatus,
    reloadConfig,
    fetchInstances,
    fetchInstancesPage,
    updateInstanceWeight,
    updateInstanceEnabled,
    fetchInterfaces
  }
})
