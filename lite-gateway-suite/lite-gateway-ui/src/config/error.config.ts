import type { ErrorCodeConfig } from '@/types/error'

// 默认错误码配置（本地兜底配置）
export const defaultErrorCodes: ErrorCodeConfig[] = [
  // 成功码 - 支持多种格式
  { code: '00000', message: '成功', level: 'info', action: 'none', success: true, showNotification: false },
  { code: '0', message: '成功', level: 'info', action: 'none', success: true, showNotification: false },

  // 认证相关
  { code: 'A0201', message: '用户账户不存在', level: 'warning', action: 'none' },
  { code: 'A0202', message: '用户账户被冻结', level: 'warning', action: 'none' },
  { code: 'A0210', message: '用户密码错误', level: 'warning', action: 'none' },
  { code: 'A0220', message: '用户身份校验失败', level: 'warning', action: 'none' },
  { code: 'A0230', message: '用户登录已过期', level: 'warning', action: 'logout' },
  { code: 'A0231', message: '用户未登录', level: 'warning', action: 'logout' },
  { code: 'A0240', message: '用户验证码错误', level: 'warning', action: 'none' },

  // 权限相关
  { code: 'A0300', message: '访问权限异常', level: 'warning', action: 'none' },
  { code: 'A0301', message: '访问未授权', level: 'warning', action: 'redirect', redirectUrl: '/403' },

  // 参数相关
  { code: 'A0400', message: '用户请求参数错误', level: 'warning', action: 'none' },
  { code: 'A0404', message: '找不到路径', level: 'warning', action: 'none' },
  { code: 'A0410', message: '请求必填参数为空', level: 'warning', action: 'none' },
  { code: 'A0421', message: '参数格式不匹配', level: 'warning', action: 'none' },

  // 系统错误
  { code: 'B0001', message: '系统执行出错', level: 'error', action: 'none' },
  { code: 'B0100', message: '系统执行超时', level: 'warning', action: 'none' },
  { code: 'B0210', message: '系统限流', level: 'warning', action: 'none' },

  // 服务错误
  { code: 'C0001', message: '调用第三方服务出错', level: 'error', action: 'none' },
  { code: 'C0300', message: '数据库服务出错', level: 'error', action: 'none' },

  // 网关错误
  { code: 'G0001', message: '网关路由不存在', level: 'warning', action: 'none' },
  { code: 'G0002', message: '网关路由已存在', level: 'warning', action: 'none' },
  { code: 'G0003', message: 'Nacos服务查询失败', level: 'error', action: 'none' },
  { code: 'G0004', message: '路由配置解析失败', level: 'error', action: 'none' },

  // HTTP状态码映射
  { code: '401', message: '登录已过期，请重新登录', level: 'warning', action: 'logout' },
  { code: '403', message: '没有权限访问', level: 'warning', action: 'none' },
  { code: '404', message: '请求的资源不存在', level: 'warning', action: 'none' },
  { code: '500', message: '服务器内部错误', level: 'error', action: 'none' },
]
