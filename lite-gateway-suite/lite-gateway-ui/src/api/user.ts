import request from '@/utils/request'
import type { 
  PageBody, 
  SysUser, 
  UserQuery,
  LoginParams,
  LoginResult,
  RefreshRequest,
  AuthConfig,
  UserInfo
} from '@/types/api'

// 用户认证管理 API - 与后端 AuthController 保持一致
export const userApi = {
  // ==================== 认证相关接口 ====================

  // 用户登录 - POST /auth/login
  login: (data: LoginParams) => {
    return request.post<any, LoginResult>('/auth/login', data)
  },

  // 用户登出 - POST /auth/logout
  logout: () => {
    return request.post<any, void>('/auth/logout')
  },

  // 获取当前用户信息 - GET /auth/user-info
  getCurrentUser: () => {
    return request.get<any, UserInfo>('/auth/user-info')
  },

  // 刷新Token - POST /auth/refresh
  refreshToken: (data: RefreshRequest) => {
    return request.post<any, LoginResult>('/auth/refresh', data)
  },

  // 获取认证配置 - GET /auth/config
  getAuthConfig: () => {
    return request.get<any, AuthConfig>('/auth/config')
  },

  // 修改密码 - PATCH /auth/password
  changePassword: (oldPassword: string, newPassword: string) => {
    return request.patch<any, void>('/auth/password', { oldPassword, newPassword })
  },

  // ==================== 用户管理接口 ====================

  // 分页查询用户列表 - GET /system/user/page
  getUserPage: (params: UserQuery) => {
    return request.get<any, PageBody<SysUser>>('/system/user/page', { params })
  },

  // 获取所有用户 - GET /system/user/list
  getUserList: () => {
    return request.get<any, SysUser[]>('/system/user/list')
  },

  // 根据ID获取用户 - GET /system/user/{id}
  getUserById: (id: number) => {
    return request.get<any, SysUser>(`/system/user/${id}`)
  },

  // 添加用户 - POST /system/user
  addUser: (data: SysUser) => {
    return request.post<any, void>('/system/user', data)
  },

  // 更新用户 - PUT /system/user/{id}
  updateUser: (id: number, data: SysUser) => {
    return request.put<any, void>(`/system/user/${id}`, data)
  },

  // 删除用户 - DELETE /system/user/{id}
  deleteUser: (id: number) => {
    return request.delete<any, void>(`/system/user/${id}`)
  },

  // 修改用户状态 - PATCH /system/user/{id}/status
  updateUserStatus: (id: number, status: number) => {
    return request.patch<any, void>(`/system/user/${id}/status`, null, {
      params: { status }
    })
  },

  // 重置密码 - PATCH /system/user/{id}/password
  resetPassword: (id: number, newPassword: string) => {
    return request.patch<any, void>(`/system/user/${id}/password`, { newPassword })
  }
}

export default userApi
