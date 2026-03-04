import { defineStore } from 'pinia'
import { ref } from 'vue'
import { rateLimitApi } from '@/api/rateLimit'
import type { RateLimitRule, RateLimitQuery } from '@/types/api'

export const useRateLimitStore = defineStore('rateLimit', () => {
  // State
  const ruleList = ref<RateLimitRule[]>([])
  const currentRule = ref<RateLimitRule | null>(null)
  const loading = ref(false)
  const total = ref(0)

  // Actions
  const fetchRateLimitPage = async (params: RateLimitQuery) => {
    loading.value = true
    try {
      const res = await rateLimitApi.getRateLimitPage(params)
      ruleList.value = res.list
      total.value = res.total
      return res
    } finally {
      loading.value = false
    }
  }

  const fetchAllRateLimits = async () => {
    loading.value = true
    try {
      const res = await rateLimitApi.getAllRateLimits()
      ruleList.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  const fetchRateLimitById = async (id: number) => {
    loading.value = true
    try {
      const res = await rateLimitApi.getRateLimitById(id)
      currentRule.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  const addRateLimit = async (data: RateLimitRule) => {
    await rateLimitApi.addRateLimit(data)
  }

  const updateRateLimit = async (id: number, data: RateLimitRule) => {
    await rateLimitApi.updateRateLimit(id, data)
  }

  const deleteRateLimit = async (id: number) => {
    await rateLimitApi.deleteRateLimit(id)
  }

  const updateRateLimitStatus = async (id: number, status: number) => {
    await rateLimitApi.updateRateLimitStatus(id, status)
  }

  const fetchRateLimitsByRouteId = async (routeId: string) => {
    const res = await rateLimitApi.getRateLimitsByRouteId(routeId)
    return res
  }

  return {
    ruleList,
    currentRule,
    loading,
    total,
    fetchRateLimitPage,
    fetchAllRateLimits,
    fetchRateLimitById,
    addRateLimit,
    updateRateLimit,
    deleteRateLimit,
    updateRateLimitStatus,
    fetchRateLimitsByRouteId
  }
})
