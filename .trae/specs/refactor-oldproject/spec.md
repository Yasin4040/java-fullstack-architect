# OldProject 重构开源化分析 Spec

## Why
现有项目包含公司特定的 OA 登录集成、内部依赖和硬编码配置，无法直接开源。需要剥离公司特定耦合代码，抽象通用接口，使其成为可插拔、可独立部署的开源网关管理系统。

## What Changes
- **剥离公司特定代码**：OA 登录集成、公司内网配置、内部 Maven 仓库依赖
- **抽象通用接口**：认证接口、服务发现接口、配置中心接口
- **插件化架构**：Nacos 支持改为可选插件形式
- **配置外部化**：硬编码 IP、公司品牌信息改为可配置
- **BREAKING**: 移除对公司内部服务的强依赖，改为接口契约

## Impact
- Affected specs: 认证流程、服务发现、路由管理、权限控制
- Affected code: 
  - `jtyjy-gateway-manage` (admin 服务)
  - `jtyjy-gateway` (gateway 服务)
  - `jtyjy-basic-parent` (公共包)
  - `cloud-platform-web` (web 前端)

---

## ADDED Requirements

### Requirement: 认证接口抽象
The system SHALL 提供统一的认证接口，支持多种认证方式插拔。

#### Scenario: 支持标准 OAuth2/JWT 认证
- **WHEN** 用户请求登录
- **THEN** 系统通过配置的认证适配器进行验证
- **AND** 返回标准格式的用户凭证

#### Scenario: 支持自定义认证扩展
- **WHEN** 开发者实现 AuthProvider 接口
- **THEN** 系统 SHALL 自动识别并加载自定义认证方式

### Requirement: 服务发现接口抽象
The system SHALL 提供服务发现抽象接口，支持 Nacos、Eureka、Consul 等多种实现。

#### Scenario: Nacos 作为可选插件
- **WHEN** 配置 `discovery.type=nacos`
- **THEN** 系统加载 Nacos 服务发现实现
- **AND** 当 Nacos 不可用时，系统优雅降级

#### Scenario: 静态服务列表支持
- **WHEN** 配置 `discovery.type=static`
- **THEN** 系统使用配置文件中的静态服务列表
- **AND** 支持运行时热更新

### Requirement: 配置中心接口抽象
The system SHALL 提供配置中心抽象，支持本地配置、Nacos Config、Apollo 等。

#### Scenario: 本地配置优先
- **WHEN** 未启用外部配置中心
- **THEN** 系统使用本地 YAML/Properties 配置
- **AND** 支持配置热加载

---

## MODIFIED Requirements

### Requirement: Admin 服务 (gateway-manage)
**现状**: 基于 Spring Boot，对接公司 OA 登录，管理 Gateway 路由配置，使用 Nacos 服务发现

**修改后**: 
- 移除公司 OA 登录集成，改为标准 OAuth2 认证
- Nacos 改为可选依赖（通过条件装配）
- 抽象 RouteRepository 接口，支持多种存储后端（MySQL/内存/Nacos）
- 移除硬编码的 Nacos 地址配置

**需要剥离的公司特定代码**:
1. `bootstrap.yml` 中的内网 Nacos 地址 `192.168.5.106:31559`
2. `jtyjy-basic` 相关的公司内部依赖
3. 公司特定的登录验证逻辑

**需要抽象的接口**:
1. `RouteDefinitionRepository` - 路由存储接口
2. `ServiceDiscoveryClient` - 服务发现接口
3. `AuthProvider` - 认证提供者接口

### Requirement: Gateway 服务 (jtyjy-gateway)
**现状**: 基于 Spring Cloud Gateway，从 Nacos 拉取服务实例，路由配置代码写死或走公司配置中心

**修改后**:
- 路由配置支持多种来源（数据库/内存/配置中心）
- 负载均衡策略可配置
- 认证过滤器支持多种 JWT 验证方式
- 白名单/黑名单管理抽象化

**需要剥离的公司特定代码**:
1. `AuthorizationManager` 中的公司特定资源 ID 验证
2. `UserDTO` 中的公司特定字段（如 `accountType` 业务含义）
3. `DevLoadBalanceRuleLoadBalancer` 中的公司特定 IP 匹配逻辑

**需要抽象的接口**:
1. `AuthorizationManager` - 权限验证接口
2. `LoadBalancerRule` - 负载均衡策略接口
3. `RouteDefinitionRepository` - 路由定义仓库接口

### Requirement: 公共包 (jtyjy-basic-parent)
**现状**: 包含多个 Starter，依赖公司内部服务和配置

**修改后**:
- 移除对公司内部 Maven 仓库的依赖
- 抽象通用功能为独立模块
- 提供清晰的扩展点

**需要剥离的公司特定代码**:
1. `jtyjy-rbac-starter` 中的 `jtyjy-rbac-export` 和 `jtyjy-user-export` 内部依赖
2. `jtyjy-springboot-starter` 中的公司内部 Nexus 地址
3. `jtyjy-security-oauth` 中的公司特定 JWT 增强逻辑

**需要抽象的接口**:
1. `UserDetailsService` - 用户信息服务接口
2. `PermissionService` - 权限服务接口
3. `TokenEnhancer` - Token 增强接口

### Requirement: Web 前端 (cloud-platform-web)
**现状**: Ant Design Pro 项目，包含公司品牌信息和特定登录流程

**修改后**:
- 品牌信息可配置（Logo、标题、副标题）
- 登录方式可配置（账号密码/SSO/扫码等）
- API 路径可配置
- 移除硬编码的公司名称

**需要剥离的公司特定代码**:
1. `Login/index.jsx` 中的公司 Logo 和名称 "金太阳云平台"
2. `AuthUserHandler.js` 中的硬编码 `CLIENT_ID` 和 `CLIENT_SECRET`
3. `services/auth/index.js` 中的公司特定 API 路径 `/auth/emp/oauth/token`
4. `app.jsx` 中的动态路由加载逻辑（依赖公司内部服务）

**需要抽象的接口**:
1. `AuthService` - 认证服务接口
2. `MenuService` - 菜单服务接口
3. `ConfigService` - 全局配置接口

---

## REMOVED Requirements

### Requirement: 公司 OA 登录集成
**Reason**: 公司特定的 OA 系统无法开源，需要替换为标准 OAuth2/OIDC 协议
**Migration**: 提供 OAuth2 适配器模板，用户可自行实现对接

### Requirement: 公司内部服务依赖
**Reason**: `jtyjy-rbac-export`、`jtyjy-user-export` 等内部包无法开源
**Migration**: 抽象为接口，用户需自行实现用户/权限服务

### Requirement: 硬编码的公司配置
**Reason**: 内网 IP、公司品牌等无法通用
**Migration**: 全部改为外部化配置，提供配置模板

---

## 代码耦合分析详细清单

### 1. Admin 服务 (gateway-manage)

| 文件 | 耦合类型 | 处理方式 |
|------|----------|----------|
| `bootstrap.yml` | 硬编码 Nacos 地址 | 改为环境变量或配置中心 |
| `pom.xml` | 依赖 `jtyjy-basic` 内部包 | 抽象接口，改为可选依赖 |
| `GatewayRouteServiceImpl.java` | 直接调用 Nacos API | 抽象 `ServiceDiscoveryClient` 接口 |
| `PermitController.java` | 通用白名单管理 | 保留，移除公司特定注解 |

### 2. Gateway 服务 (jtyjy-gateway)

| 文件 | 耦合类型 | 处理方式 |
|------|----------|----------|
| `AuthorizationManager.java` | 公司特定资源 ID 验证 | 抽象 `AuthorizationProvider` 接口 |
| `AuthGlobalFilter.java` | JWT 解析逻辑 | 保留，支持多种 Token 格式 |
| `UserDTO.java` | 公司特定用户字段 | 改为通用字段，扩展字段用 Map |
| `MysqlRouteDefinitionRepository.java` | 数据库路由存储 | 保留，抽象 `RouteRepository` 接口 |
| `DevLoadBalanceRuleLoadBalancer.java` | 公司特定负载均衡 | 抽象 `LoadBalancerStrategy` 接口 |
| `bootstrap.yml` | 硬编码 Nacos 地址 | 改为环境变量或配置中心 |

### 3. 公共包 (jtyjy-basic-parent)

| 模块 | 耦合类型 | 处理方式 |
|------|----------|----------|
| `jtyjy-security-oauth` | 公司特定 JWT 增强 | 抽象 `TokenEnhancer` 接口 |
| `jtyjy-rbac-starter` | 依赖内部 rbac/user 服务 | 抽象 `UserService`、`PermissionService` 接口 |
| `jtyjy-springboot-starter` | 公司内部 Nexus 配置 | 移除，改为 Maven Central |
| `jtyjy-eventbus-rocketmq` | 特定 MQ 实现 | 改为可选插件 |

### 4. Web 前端 (cloud-platform-web)

| 文件 | 耦合类型 | 处理方式 |
|------|----------|----------|
| `Login/index.jsx` | 公司品牌信息 | 改为配置文件注入 |
| `AuthUserHandler.js` | 硬编码客户端凭证 | 改为环境变量或配置 |
| `services/auth/index.js` | 公司特定 API 路径 | 改为可配置的基础路径 |
| `app.jsx` | 动态路由依赖内部服务 | 抽象路由加载接口 |
| `config/proxy.js` | 硬编码代理地址 | 改为环境变量 |

---

## 建议的接口抽象清单

### 后端接口

1. **认证相关**
   - `AuthProvider` - 认证提供者接口
   - `UserDetailsService` - 用户详情服务
   - `TokenEnhancer` - Token 增强器

2. **服务发现相关**
   - `ServiceDiscoveryClient` - 服务发现客户端
   - `ServiceInstance` - 服务实例模型
   - `ServiceRegistry` - 服务注册接口

3. **路由管理相关**
   - `RouteRepository` - 路由存储接口
   - `RouteDefinition` - 路由定义模型
   - `RouteEventPublisher` - 路由变更事件发布

4. **权限相关**
   - `AuthorizationProvider` - 授权提供者
   - `PermissionService` - 权限服务
   - `WhiteListProvider` - 白名单提供者

### 前端接口

1. **认证相关**
   - `AuthService` - 认证服务
   - `TokenStorage` - Token 存储

2. **菜单/路由相关**
   - `MenuService` - 菜单服务
   - `RouteLoader` - 路由加载器

3. **配置相关**
   - `AppConfig` - 应用配置
   - `BrandConfig` - 品牌配置
