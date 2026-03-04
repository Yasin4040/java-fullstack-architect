import axios, { AxiosInstance, AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/store/modules/user'
import { errorConfigService } from '@/services/errorConfigService'
import { useErrorHandler } from '@/hooks/useErrorHandler'

// 不需要 Token 的白名单接口（支持字符串或正则）
const WHITE_LIST = [
  '/login',
  '/auth/login',
  '/auth/register',
  '/auth/refresh-token',
  '/auth/forgot-password',
  '/auth/captcha'
]


// 检查 URL 是否在白名单中
const isInWhiteList = (url: string): boolean => {
  return WHITE_LIST.some(pattern => {
    if (typeof pattern === 'string') {
      return url.includes(pattern)
    }
    return pattern.test(url)
  })
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

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response

    // 如果响应不是标准格式（没有 code 字段），直接返回原始数据
    if (data.code === undefined) {
      return data
    }

    // 处理业务错误 - 使用配置的成功码判断
    if (!errorConfigService.isSuccessCode(data.code)) {
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
    const { response } = error
    const { handleHttpError } = useErrorHandler()

    if (response) {
      const { status, data } = response
      handleHttpError(status, (data as { message?: string })?.message)
    } else {
      message.error('网络连接失败，请检查网络')
    }

    return Promise.reject(error)
  }
)

export default request
