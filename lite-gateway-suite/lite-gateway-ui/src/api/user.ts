import request from '@/utils/request'
import type { 
  PageResult, 
  SysUser, 
  UserQuery,
  LoginParams,
  LoginResult,
  RefreshRequest,
  AuthConfig,
  UserInfo
} from '@/types/api'

// 用户管理 API
export const userApi = {
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

  // 分页查询用户列表
  getUserPage: (params: UserQuery) => {
    return request.get<any, PageResult<SysUser>>('/system/user/page', { params })
  },

  // 获取所有用户
  getAllUsers: () => {
    return request.get<any, SysUser[]>('/system/user/list')
  },

  // 根据ID获取用户
  getUserById: (id: number) => {
    return request.get<any, SysUser>(`/system/user/${id}`)
  },

  // 添加用户
  addUser: (data: SysUser) => {
    return request.post<any, void>('/system/user', data)
  },

  // 更新用户
  updateUser: (id: number, data: SysUser) => {
    return request.put<any, void>(`/system/user/${id}`, data)
  },

  // 删除用户
  deleteUser: (id: number) => {
    return request.delete<any, void>(`/system/user/${id}`)
  },

  // 修改用户状态
  updateUserStatus: (id: number, status: number) => {
    return request.patch<any, void>(`/system/user/${id}/status`, null, {
      params: { status }
    })
  },

  // 重置密码
  resetPassword: (id: number, newPassword: string) => {
    return request.patch<any, void>(`/system/user/${id}/password`, { newPassword })
  },

  // 修改密码
  changePassword: (oldPassword: string, newPassword: string) => {
    return request.patch<any, void>('/auth/password', { oldPassword, newPassword })
  }
}

export default userApi
