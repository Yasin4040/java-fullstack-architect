# OldProject 重构开源化 Tasks

## 阶段一：基础架构抽象

- [ ] Task 1: 创建核心抽象模块 (gateway-core-api)
  - [ ] SubTask 1.1: 定义 RouteRepository 接口
  - [ ] SubTask 1.2: 定义 ServiceDiscoveryClient 接口
  - [ ] SubTask 1.3: 定义 AuthProvider 接口
  - [ ] SubTask 1.4: 定义 AuthorizationProvider 接口

- [ ] Task 2: 创建 Nacos 插件模块 (gateway-plugin-nacos)
  - [ ] SubTask 2.1: 实现 NacosServiceDiscoveryClient
  - [ ] SubTask 2.2: 实现 NacosRouteRepository
  - [ ] SubTask 2.3: 添加条件装配配置

- [ ] Task 3: 创建本地配置插件模块 (gateway-plugin-local)
  - [ ] SubTask 3.1: 实现 LocalRouteRepository（基于 YAML/内存）
  - [ ] SubTask 3.2: 实现 StaticServiceDiscoveryClient
  - [ ] SubTask 3.3: 添加配置文件模板

## 阶段二：Admin 服务重构

- [ ] Task 4: 重构 gateway-manage 服务
  - [ ] SubTask 4.1: 移除 jtyjy-basic 内部依赖
  - [ ] SubTask 4.2: 替换硬编码 Nacos 地址为配置注入
  - [ ] SubTask 4.3: 集成 gateway-core-api 接口
  - [ ] SubTask 4.4: 添加多存储后端支持（MySQL/内存）

- [ ] Task 5: 抽象认证层
  - [ ] SubTask 5.1: 创建 OAuth2AuthProvider 实现
  - [ ] SubTask 5.2: 移除公司 OA 登录代码
  - [ ] SubTask 5.3: 添加标准 JWT 验证支持

## 阶段三：Gateway 服务重构

- [ ] Task 6: 重构 gateway 服务核心
  - [ ] SubTask 6.1: 集成 gateway-core-api 接口
  - [ ] SubTask 6.2: 重构 AuthorizationManager 为接口驱动
  - [ ] SubTask 6.3: 抽象 UserDTO 为通用模型
  - [ ] SubTask 6.4: 替换硬编码配置

- [ ] Task 7: 负载均衡策略抽象
  - [ ] SubTask 7.1: 定义 LoadBalancerStrategy 接口
  - [ ] SubTask 7.2: 重构 DevLoadBalanceRuleLoadBalancer
  - [ ] SubTask 7.3: 添加策略配置支持

## 阶段四：公共包重构

- [ ] Task 8: 重构 jtyjy-security-oauth 模块
  - [ ] SubTask 8.1: 抽象 TokenEnhancer 接口
  - [ ] SubTask 8.2: 移除公司特定 JWT 增强逻辑
  - [ ] SubTask 8.3: 提供标准 JWT 实现

- [ ] Task 9: 清理和简化公共包
  - [ ] SubTask 9.1: 移除 jtyjy-rbac-starter 内部依赖
  - [ ] SubTask 9.2: 抽象 UserService/PermissionService 接口
  - [ ] SubTask 9.3: 移除公司内部 Nexus 配置
  - [ ] SubTask 9.4: 将 eventbus-rocketmq 改为可选模块

## 阶段五：Web 前端重构

- [ ] Task 10: 重构前端配置层
  - [ ] SubTask 10.1: 创建 ConfigService 配置服务
  - [ ] SubTask 10.2: 将品牌信息（Logo、标题）改为配置注入
  - [ ] SubTask 10.3: 将 API 基础路径改为可配置

- [ ] Task 11: 重构前端认证层
  - [ ] SubTask 11.1: 抽象 AuthService 接口
  - [ ] SubTask 11.2: 移除硬编码 CLIENT_ID/CLIENT_SECRET
  - [ ] SubTask 11.3: 支持多种登录方式配置

- [ ] Task 12: 重构前端路由层
  - [ ] SubTask 12.1: 抽象 MenuService 接口
  - [ ] SubTask 12.2: 支持静态/动态路由配置
  - [ ] SubTask 12.3: 移除公司特定路由加载逻辑

## 阶段六：文档和示例

- [ ] Task 13: 编写重构文档
  - [ ] SubTask 13.1: 编写架构设计文档
  - [ ] SubTask 13.2: 编写接口扩展指南
  - [ ] SubTask 13.3: 编写部署配置指南

- [ ] Task 14: 创建示例项目
  - [ ] SubTask 14.1: 创建最小化启动示例
  - [ ] SubTask 14.2: 创建自定义认证扩展示例
  - [ ] SubTask 14.3: 创建自定义服务发现扩展示例

## Task Dependencies

- Task 4 依赖 Task 1, Task 2
- Task 5 依赖 Task 1
- Task 6 依赖 Task 1, Task 2
- Task 7 依赖 Task 6
- Task 8 依赖 Task 1
- Task 9 依赖 Task 1
- Task 11 依赖 Task 10
- Task 12 依赖 Task 10
- Task 13 依赖 Task 4, Task 6, Task 9
- Task 14 依赖 Task 13
