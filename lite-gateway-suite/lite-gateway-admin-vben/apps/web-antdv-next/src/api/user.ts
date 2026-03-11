import { requestClient } from './request'
import type { 
  PageBody, 
  LoginParams, 
  LoginResult, 
  UserInfo, 
  SysUser, 
  UserQuery, 
  RefreshRequest, 
  AuthConfig 
} from '@/types/api'

// 用户管理 API
export const userApi = {
  // 登录
  login: (data: LoginParams) => {
    return requestClient.post<LoginResult>('/gateway/auth/login', data)
  },

  // 登出
  logout: () => {
    return requestClient.post<void>('/gateway/auth/logout')
  },

  // 刷新token
  refreshToken: (data: RefreshRequest) => {
    return requestClient.post<string>('/gateway/auth/refresh', data)
  },

  // 获取当前用户信息
  getCurrentUser: () => {
    return requestClient.get<UserInfo>('/gateway/auth/info')
  },

  // 获取认证配置
  getAuthConfig: () => {
    return requestClient.get<AuthConfig>('/gateway/auth/config')
  },

  // ==================== 用户管理接口 ====================

  // 分页查询用户列表
  getUserPage: (params: UserQuery) => {
    return requestClient.get<PageBody<SysUser>>('/gateway/user/page', { params })
  },

  // 获取所有用户
  getAllUsers: () => {
    return requestClient.get<SysUser[]>('/gateway/user/list')
  },

  // 根据ID获取用户
  getUserById: (id: number) => {
    return requestClient.get<SysUser>(`/gateway/user/${id}`)
  },

  // 添加用户
  addUser: (data: SysUser) => {
    return requestClient.post<void>('/gateway/user', data)
  },

  // 更新用户
  updateUser: (id: number, data: SysUser) => {
    return requestClient.put<void>(`/gateway/user/${id}`, data)
  },

  // 删除用户
  deleteUser: (id: number) => {
    return requestClient.delete<void>(`/gateway/user/${id}`)
  },

  // 修改用户状态
  updateUserStatus: (id: number, status: number) => {
    return requestClient.patch<void>(`/gateway/user/${id}/status`, null, {
      params: { status }
    })
  },

  // 重置用户密码
  resetPassword: (id: number, password: string) => {
    return requestClient.post<void>('/gateway/user/reset-password', {
      id,
      password
    })
  }
}

export default userApi
