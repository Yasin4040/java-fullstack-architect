import { message } from 'ant-design-vue'
import type { ErrorCodeConfig, ApiError } from '@/types/error'
import { errorConfigService } from '@/services/errorConfigService'
import { useUserStore } from '@/store/modules/user'
import router from '@/router'

/**
 * 错误处理Hook
 * 根据错误码配置统一处理错误
 */
export function useErrorHandler() {
  /**
   * 处理业务错误
   */
  const handleBusinessError = (error: ApiError): void => {
    const config = errorConfigService.getConfig(error.code)

    if (!config) {
      // 未配置的错误，使用默认处理
      message.error(error.message || '请求失败')
      return
    }

    // 显示通知
    if (config.showNotification !== false) {
      const msg = error.message || config.message
      const duration = config.duration || 3

      switch (config.level) {
        case 'info':
          message.info(msg, duration)
          break
        case 'warning':
          message.warning(msg, duration)
          break
        case 'error':
        case 'fatal':
          message.error(msg, duration)
          break
        default:
          message.error(msg, duration)
      }
    }

    // 执行配置的动作
    executeAction(config, error)
  }

  /**
   * 处理HTTP错误
   */
  const handleHttpError = (status: number, errorMsg?: string): void => {
    // 尝试从配置中获取错误码
    const config = errorConfigService.getConfig(String(status))

    if (config) {
      handleBusinessError({
        code: String(status),
        message: errorMsg || config.message
      })
      return
    }

    // 默认HTTP错误处理
    switch (status) {
      case 401:
        message.error('登录已过期，请重新登录')
        handleLogout()
        break
      case 403:
        message.error('没有权限访问')
        break
      case 404:
        message.error('请求的资源不存在')
        break
      case 500:
        message.error('服务器内部错误')
        break
      default:
        message.error(errorMsg || `请求失败: ${status}`)
    }
  }

  /**
   * 执行错误码配置的动作
   */
  const executeAction = (config: ErrorCodeConfig, error: ApiError): void => {
    switch (config.action) {
      case 'logout':
        handleLogout()
        break
      case 'redirect':
        if (config.redirectUrl) {
          router.push(config.redirectUrl)
        }
        break
      case 'retry':
        // 重试逻辑由调用方处理
        console.log('[ErrorHandler] Retry action triggered for code:', error.code)
        break
      case 'none':
      default:
        // 不执行任何动作
        break
    }
  }

  /**
   * 处理登出
   */
  const handleLogout = (): void => {
    const userStore = useUserStore()
    userStore.logout()
    router.push('/login')
  }

  /**
   * 获取错误消息（不显示通知，仅返回消息）
   */
  const getErrorMessage = (code: string, defaultMsg?: string): string => {
    const config = errorConfigService.getConfig(code)
    return config?.message || defaultMsg || '操作失败'
  }

  return {
    handleBusinessError,
    handleHttpError,
    getErrorMessage
  }
}

export default useErrorHandler
