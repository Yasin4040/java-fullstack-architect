import axios, { AxiosInstance, AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/store/modules/user'
import { errorConfigService } from '@/services/errorConfigService'
import { useErrorHandler } from '@/hooks/useErrorHandler'

// 不需要 Token 的白名单接口
const WHITE_LIST: string[] = [
  '/login',
  '/auth/login',
  '/auth/register',
  '/auth/refresh-token',
  '/auth/forgot-password',
  '/auth/captcha'
]

// 静默错误处理的接口（不显示错误消息）
const SILENT_ERROR_LIST: string[] = [
  '/auth/config',
  '/config/error-codes'
]


// 检查 URL 是否在白名单中
const isInWhiteList = (url: string): boolean => {
  return WHITE_LIST.some(pattern => url.includes(pattern))
}

// 检查 URL 是否需要静默处理错误
const isSilentError = (url: string | undefined): boolean => {
  if (!url) return false
  return SILENT_ERROR_LIST.some(pattern => url.includes(pattern))
}

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()

    // 只对不在白名单中的请求添加 Token
    if (config.url && !isInWhiteList(config.url)) {
      if (userStore.token) {
        config.headers.Authorization = `Bearer ${userStore.token}`
      }
    }

    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// 默认成功码判断（用于 errorConfigService 初始化前）
const isSuccessCodeDefault = (code: string | number): boolean => {
  const codeStr = String(code)
  return ['00000', '0', '200', 'success', 'ok'].includes(codeStr.toLowerCase())
}

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response

    // 如果响应不是标准格式（没有 code 字段），直接返回原始数据
    if (data.code === undefined) {
      return data
    }

    // 处理业务错误 - 使用配置的成功码判断（如果服务未加载完成，使用默认判断）
    const isSuccess = errorConfigService.isLoaded()
      ? errorConfigService.isSuccessCode(data.code)
      : isSuccessCodeDefault(data.code)

    if (!isSuccess) {
      const { handleBusinessError } = useErrorHandler()
      handleBusinessError({
        code: String(data.code),
        message: data.message,
        data: data.data
      })
      return Promise.reject(data)
    }

    // 返回 data 字段的内容（解包 Result 包装）
    return data.data
  },
  (error: AxiosError) => {
    const { response, config } = error
    const { handleHttpError } = useErrorHandler()

    // 检查是否需要静默处理
    const silent = isSilentError(config?.url)

    if (response) {
      const { status, data } = response
      if (!silent) {
        handleHttpError(status, (data as { message?: string })?.message)
      }
    } else {
      if (!silent) {
        message.error('网络连接失败，请检查网络')
      }
    }

    return Promise.reject(error)
  }
)

export default request
