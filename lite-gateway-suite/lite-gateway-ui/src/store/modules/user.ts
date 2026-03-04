import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'
import type { UserInfo, LoginParams, LoginResult } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref(false)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const userRoles = computed(() => userInfo.value?.roles || [])
  const userPermissions = computed(() => userInfo.value?.permissions || [])

  // Actions
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setRefreshToken = (newRefreshToken: string) => {
    refreshToken.value = newRefreshToken
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  const clearToken = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  const login = async (params: LoginParams) => {
    loading.value = true
    try {
      const res: LoginResult = await userApi.login(params)
      setToken(res.accessToken)
      setRefreshToken(res.refreshToken)
      userInfo.value = res.userInfo
      return res
    } finally {
      loading.value = false
    }
  }

  const logout = async () => {
    try {
      await userApi.logout()
    } finally {
      clearToken()
    }
  }

  const fetchUserInfo = async () => {
    if (!token.value) return null
    try {
      const res: UserInfo = await userApi.getCurrentUser()
      userInfo.value = res
      return res
    } catch (error) {
      clearToken()
      return null
    }
  }

  const refreshAccessToken = async () => {
    if (!refreshToken.value) {
      clearToken()
      return null
    }
    try {
      const res: LoginResult = await userApi.refreshToken({ refreshToken: refreshToken.value })
      setToken(res.accessToken)
      setRefreshToken(res.refreshToken)
      userInfo.value = res.userInfo
      return res
    } catch (error) {
      clearToken()
      return null
    }
  }

  return {
    token,
    refreshToken,
    userInfo,
    loading,
    isLoggedIn,
    username,
    userRoles,
    userPermissions,
    login,
    logout,
    fetchUserInfo,
    refreshAccessToken,
    setToken,
    setRefreshToken,
    clearToken
  }
})
