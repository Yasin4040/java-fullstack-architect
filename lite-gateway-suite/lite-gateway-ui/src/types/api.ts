// ==================== 通用响应类型 ====================

// 通用响应结果 - 与后端 Result<T> 保持一致
export interface ApiResponse<T = any> {
  code: string
  message: string
  data: T
}

// 分页响应体 - 与后端 PageBody<T> 保持一致
export interface PageBody<T> {
  total: number
  pages: number
  pageSize: number
  pageNum: number
  list: T[]
}

// 分页查询参数 - 与后端 PageQuery 保持一致
export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

// ==================== 路由相关类型 ====================

// 路由查询参数 - 与后端 RouteQuery 保持一致
export interface RouteQuery extends PageQuery {
  status?: string
  name?: string
  uri?: string
  path?: string
}

// 路由视图对象 - 与后端 RouteVO 保持一致（用于列表/分页返回）
export interface RouteVO {
  id: number
  systemCode?: string
  name: string
  uri: string
  path?: string
  stripPrefix?: number
  filterRateLimiterName?: string
  status: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

// 路由数据传输对象 - 与后端 RouteDTO 保持一致（用于详情/编辑）
export interface RouteDTO {
  id?: number
  systemCode?: string
  name: string
  uri: string
  path?: string
  stripPrefix?: number
  host?: string
  remoteAddr?: string
  header?: string
  filterRateLimiterName?: string
  replenishRate?: number
  burstCapacity?: number
  status?: string
  requestParameter?: string
  rewritePath?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
  weightName?: string
  weight?: number
}

// 前端使用的 Route 类型（兼容 RouteVO 和 RouteDTO）
export interface Route extends RouteVO {
  // 额外字段来自 RouteDTO
  host?: string
  remoteAddr?: string
  header?: string
  replenishRate?: number
  burstCapacity?: number
  requestParameter?: string
  rewritePath?: string
  weightName?: string
  weight?: number
}

// 实例查询参数 - 与后端 InstanceQuery 保持一致
export interface InstanceQuery extends PageQuery {
  serviceName?: string
  groupName?: string
  clusterName?: string
}

// 实例数据传输对象 - 与后端 InstanceDTO 保持一致
export interface InstanceDTO {
  instanceId?: string
  serviceName: string
  enabled?: boolean
  weight?: number
}

// Nacos 实例信息（后端返回的 Instance 类型）
export interface Instance {
  instanceId: string
  ip: string
  port: number
  weight: number
  healthy: boolean
  enabled: boolean
  ephemeral: boolean
  clusterName: string
  serviceName: string
  metadata: Record<string, string>
}

// 接口信息传输对象 - 与后端 InterfaceDTO 保持一致
export interface InterfaceDTO {
  path: string
  summary?: string
  type?: string
  tag?: string
  ifAdd?: string
}

// ==================== 用户认证相关类型 ====================

// 登录请求参数 - 与后端 LoginRequest 保持一致
export interface LoginParams {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
}

// 登录响应结果 - 与后端 LoginResponse 保持一致
export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

// 用户信息 - 与后端 UserInfo 保持一致
export interface UserInfo {
  userId: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

// 系统用户类型 - 用于用户管理
export interface SysUser {
  id?: number
  username: string
  password?: string
  realName?: string
  email?: string
  phone?: string
  status?: number
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
}

// 用户查询参数
export interface UserQuery extends PageQuery {
  username?: string
  realName?: string
  status?: number | string
}

// Token 刷新请求
export interface RefreshRequest {
  refreshToken: string
}

// 认证配置响应 - 与后端 AuthConfigResponse 保持一致
export interface AuthConfig {
  authType: string
  oauth2Enabled: boolean
  ldapEnabled: boolean
  oauth2AuthorizationUri?: string
}

// ==================== 限流规则相关类型 ====================

// 限流规则查询参数
export interface RateLimitQuery extends PageQuery {
  ruleName?: string
  routeId?: string
  limitType?: number | string
  status?: number | string
}

// 限流规则类型
export interface RateLimitRule {
  id?: number
  ruleName: string
  routeId?: string
  limitType: number
  keyPrefix?: string
  replenishRate: number
  burstCapacity: number
  requestedTokens?: number
  status: number
  createTime?: string
  updateTime?: string
}

// 限流类型枚举
export enum LimitType {
  IP = 1,
  USER = 2,
  GLOBAL = 3
}

// ==================== 日志相关类型 ====================

// 日志查询参数
export interface LogQuery extends PageQuery {
  routeId?: string
  path?: string
  method?: string
  clientIp?: string
  statusCode?: number | string
  startTime?: string
  endTime?: string
}

// 网关日志
export interface GatewayLog {
  id: string
  routeId: string
  path: string
  method: string
  clientIp: string
  userId?: string
  requestTime: number
  responseTime: number
  duration: number
  statusCode: number
  requestSize: number
  responseSize: number
  errorMsg?: string
}

// 日志统计
export interface LogStatistics {
  totalRequests: number
  successCount: number
  errorCount: number
  avgResponseTime: number
  qps: number
}

// 路由访问排行
export interface RouteRank {
  routeId: string
  count: number
  avgResponseTime: number
  errorRate: number
}

// 状态码分布
export interface StatusDistribution {
  statusCode: number
  count: number
  percentage: number
}

// 响应时间趋势
export interface ResponseTimeTrend {
  time: string
  avgResponseTime: number
  maxResponseTime: number
  minResponseTime: number
}

// ==================== 网关配置相关类型 ====================

// 网关配置 DTO
export interface GatewayConfigDTO {
  version: string
  routes: RouteDTO[]
  ipBlacklist: string[]
  whiteList: string[]
}

// 白名单
export interface WhiteList {
  id?: number
  path: string
  description?: string
  createTime?: string
}

// IP 黑名单
export interface IpBlacklist {
  id?: number
  ip: string
  description?: string
  createTime?: string
}
