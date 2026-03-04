import type { ErrorCodeConfig } from '@/types/error'
import { defaultErrorCodes } from '@/config/error.config'
import request from '@/utils/request'

class ErrorConfigService {
  private configMap: Map<string, ErrorCodeConfig> = new Map()
  private successCode: string = '00000'
  private loaded: boolean = false

  /**
   * 初始化：加载错误码配置
   * 先加载本地默认配置，再尝试从后端拉取覆盖
   */
  async init(): Promise<void> {
    if (this.loaded) return

    // 1. 先加载本地默认配置（保底）
    this.loadLocalConfig()

    // 2. 尝试从后端拉取配置
    try {
      const response = await request({
        url: '/config/error-codes',
        method: 'get'
      })

      if (response && Array.isArray(response)) {
        this.mergeServerConfig(response)
        // 查找成功码
        this.findSuccessCode(response)
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
      const response = await request({
        url: '/config/error-codes',
        method: 'get'
      })

      if (response && Array.isArray(response)) {
        this.configMap.clear()
        this.loadLocalConfig()
        this.mergeServerConfig(response)
        this.findSuccessCode(response)
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
   */
  isSuccessCode(code: string | number): boolean {
    return String(code) === this.successCode
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
