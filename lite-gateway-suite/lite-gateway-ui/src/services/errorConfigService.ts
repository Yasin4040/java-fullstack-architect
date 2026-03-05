import type { ErrorCodeConfig } from '@/types/error'
import { defaultErrorCodes } from '@/config/error.config'
import axios from 'axios'

class ErrorConfigService {
  private configMap: Map<string, ErrorCodeConfig> = new Map()
  private successCode: string = '00000'
  private loaded: boolean = false

  /**
   * 初始化：加载错误码配置
   * 先加载本地默认配置，再尝试从后端拉取覆盖
   * 注意：使用原生 axios 避免循环依赖
   */
  async init(): Promise<void> {
    if (this.loaded) return

    // 1. 先加载本地默认配置（保底）
    this.loadLocalConfig()

    // 2. 尝试从后端拉取配置（使用原生 axios，避免循环依赖）
    try {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
      const response = await axios.get(`${baseURL}/config/error-codes`, {
        timeout: 5000,
        headers: {
          'Content-Type': 'application/json'
        }
      })

      // 直接获取响应数据（不需要经过拦截器处理）
      const data = response.data
      if (data && Array.isArray(data.data)) {
        this.mergeServerConfig(data.data)
        // 查找成功码
        this.findSuccessCode(data.data)
        console.log('[ErrorConfig] Loaded from server, total:', this.configMap.size, 'success code:', this.successCode)
      } else if (Array.isArray(data)) {
        // 兼容直接返回数组的情况
        this.mergeServerConfig(data)
        this.findSuccessCode(data)
        console.log('[ErrorConfig] Loaded from server, total:', this.configMap.size, 'success code:', this.successCode)
      }
    } catch (e) {
      console.warn('[ErrorConfig] Failed to load from server, using local default')
    }

    this.loaded = true
  }

  /**
   * 加载本地默认配置
   */
  private loadLocalConfig(): void {
    for (const config of defaultErrorCodes) {
      this.configMap.set(config.code, config)
    }
  }

  /**
   * 合并后端配置（后端配置优先级更高）
   */
  private mergeServerConfig(serverConfigs: ErrorCodeConfig[]): void {
    for (const config of serverConfigs) {
      if (config.code) {
        this.configMap.set(config.code, config)
      }
    }
  }

  /**
   * 查找成功码
   */
  private findSuccessCode(configs: ErrorCodeConfig[]): void {
    for (const config of configs) {
      if (config.success === true) {
        this.successCode = config.code
        return
      }
    }
    // 默认使用 00000
    this.successCode = '00000'
  }

  /**
   * 手动刷新配置
   */
  async refresh(): Promise<boolean> {
    try {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
      const response = await axios.get(`${baseURL}/config/error-codes`, {
        timeout: 5000,
        headers: {
          'Content-Type': 'application/json'
        }
      })

      const data = response.data
      let configs: ErrorCodeConfig[] = []
      
      if (data && Array.isArray(data.data)) {
        configs = data.data
      } else if (Array.isArray(data)) {
        configs = data
      }

      if (configs.length > 0) {
        this.configMap.clear()
        this.loadLocalConfig()
        this.mergeServerConfig(configs)
        this.findSuccessCode(configs)
        return true
      }
      return false
    } catch (e) {
      console.error('[ErrorConfig] Refresh failed:', e)
      return false
    }
  }

  /**
   * 根据错误码获取配置
   */
  getConfig(code: string | number): ErrorCodeConfig | undefined {
    return this.configMap.get(String(code))
  }

  /**
   * 获取成功码
   */
  getSuccessCode(): string {
    return this.successCode
  }

  /**
   * 判断是否为成功码
   * 兼容数字和字符串类型的比较
   */
  isSuccessCode(code: string | number): boolean {
    const codeStr = String(code)
    // 1. 直接匹配配置的成功码
    if (codeStr === this.successCode) {
      return true
    }
    // 2. 兼容常见的成功码格式
    const commonSuccessCodes = ['00000', '0', '200', 'success', 'ok']
    if (commonSuccessCodes.includes(codeStr.toLowerCase())) {
      return true
    }
    // 3. 检查配置中是否标记为 success: true
    const config = this.configMap.get(codeStr)
    if (config?.success === true) {
      return true
    }
    return false
  }

  /**
   * 获取所有配置
   */
  getAllConfigs(): ErrorCodeConfig[] {
    return Array.from(this.configMap.values())
  }

  /**
   * 判断是否已加载
   */
  isLoaded(): boolean {
    return this.loaded
  }
}

export const errorConfigService = new ErrorConfigService()
