import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logApi } from '@/api/log'
import type { GatewayLog, LogQuery } from '@/types/api'

export const useLogStore = defineStore('log', () => {
  // State
  const logList = ref<GatewayLog[]>([])
  const currentLog = ref<GatewayLog | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const statistics = ref<{
    totalRequests: number
    successCount: number
    errorCount: number
    avgResponseTime: number
    qps: number
  } | null>(null)
  const routeRank = ref<Array<{
    routeId: string
    count: number
    avgResponseTime: number
    errorRate: number
  }>>([])
  const statusDistribution = ref<Array<{
    statusCode: number
    count: number
    percentage: number
  }>>([])
  const responseTimeTrend = ref<Array<{
    time: string
    avgResponseTime: number
    maxResponseTime: number
    minResponseTime: number
  }>>([])

  // Actions
  const fetchLogPage = async (params: LogQuery) => {
    loading.value = true
    try {
      const res = await logApi.getLogPage(params)
      logList.value = res.list
      total.value = res.total
      return res
    } finally {
      loading.value = false
    }
  }

  const fetchLogById = async (id: string) => {
    loading.value = true
    try {
      const res = await logApi.getLogById(id)
      currentLog.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  const fetchStatistics = async (params: { startTime?: string; endTime?: string }) => {
    const res = await logApi.getLogStatistics(params)
    statistics.value = res
    return res
  }

  const fetchRouteRank = async (params: { startTime?: string; endTime?: string; top?: number }) => {
    const res = await logApi.getRouteRank(params)
    routeRank.value = res
    return res
  }

  const fetchStatusDistribution = async (params: { startTime?: string; endTime?: string }) => {
    const res = await logApi.getStatusDistribution(params)
    statusDistribution.value = res
    return res
  }

  const fetchResponseTimeTrend = async (params: { startTime?: string; endTime?: string; interval?: string }) => {
    const res = await logApi.getResponseTimeTrend(params)
    responseTimeTrend.value = res
    return res
  }

  return {
    logList,
    currentLog,
    loading,
    total,
    statistics,
    routeRank,
    statusDistribution,
    responseTimeTrend,
    fetchLogPage,
    fetchLogById,
    fetchStatistics,
    fetchRouteRank,
    fetchStatusDistribution,
    fetchResponseTimeTrend
  }
})
