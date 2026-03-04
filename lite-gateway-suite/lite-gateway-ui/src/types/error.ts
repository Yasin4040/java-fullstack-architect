// 错误码配置类型
export interface ErrorCodeConfig {
  code: string
  message: string
  level: 'info' | 'warning' | 'error' | 'fatal'
  action: 'none' | 'logout' | 'redirect' | 'retry'
  redirectUrl?: string
  showNotification?: boolean
  duration?: number
  logStackTrace?: boolean
  success?: boolean  // 是否为成功码
}

// API错误类型
export interface ApiError {
  code: string
  message: string
  data?: any
}
