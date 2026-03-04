import { defineStore } from 'pinia'
import { ref } from 'vue'
import { routeApi } from '@/api/route'
import type { Route, RouteQuery, Instance, InterfaceInfo } from '@/types/api'

export const useRouteStore = defineStore('route', () => {
  // State
  const routeList = ref<Route[]>([])
  const currentRoute = ref<Route | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const instances = ref<Instance[]>([])
  const interfaces = ref<InterfaceInfo[]>([])

  // Actions
  const fetchRoutePage = async (params: RouteQuery) => {
    loading.value = true
    try {
      const res = await routeApi.getRoutePage(params)
      routeList.value = res.list
      total.value = res.total
      return res
    } finally {
      loading.value = false
    }
  }

  const fetchAllRoutes = async () => {
    loading.value = true
    try {
      const res = await routeApi.getAllRoutes()
      routeList.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  const fetchRouteById = async (id: number) => {
    loading.value = true
    try {
      const res = await routeApi.getRouteById(id)
      currentRoute.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  const addRoute = async (data: Route) => {
    await routeApi.addRoute(data)
  }

  const updateRoute = async (id: number, data: Route) => {
    await routeApi.updateRoute(id, data)
  }

  const deleteRoute = async (id: number) => {
    await routeApi.deleteRoute(id)
  }

  const updateRouteStatus = async (id: number, status: number) => {
    await routeApi.updateRouteStatus(id, status)
  }

  const reloadConfig = async () => {
    await routeApi.reloadConfig()
  }

  const fetchInstances = async (serviceName: string) => {
    const res = await routeApi.getInstances(serviceName)
    instances.value = res
    return res
  }

  const fetchInterfaces = async (serviceName: string) => {
    const res = await routeApi.getInterfaces(serviceName)
    interfaces.value = res
    return res
  }

  return {
    routeList,
    currentRoute,
    loading,
    total,
    instances,
    interfaces,
    fetchRoutePage,
    fetchAllRoutes,
    fetchRouteById,
    addRoute,
    updateRoute,
    deleteRoute,
    updateRouteStatus,
    reloadConfig,
    fetchInstances,
    fetchInterfaces
  }
})
