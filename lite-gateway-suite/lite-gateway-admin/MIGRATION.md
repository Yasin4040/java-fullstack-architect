# 代码迁移说明

## 概述
本文档记录了从旧项目 `jtyjy-gateway-manage` 迁移代码到 `lite-gateway-admin` 的过程和变更。

## 迁移清单

### 1. 包名修改
- **旧包名**: `com.jtyjy.gateway`
- **新包名**: `com.litegateway.admin`

所有 Java 文件的 package 声明和 import 语句都已更新。

### 2. 移除的公司特定依赖

#### 2.1 移除的注解
- `@CorpAuth` - 公司认证注解
- `@DepartmentCheck` - 部门检查注解

这些注解在 Controller 中已被移除，改用标准的 Spring Security 机制。

#### 2.2 替换的工具类
- **旧**: `CompanyUserUtil.getCurrentUser()` - 从公司内部系统获取当前用户
- **新**: `JwtUtils` - 从 JWT Token 解析用户信息

新实现位于: `com.litegateway.admin.utils.JwtUtils`

### 3. 技术栈更新

#### 3.1 API 文档
- **旧**: Swagger 2 (io.swagger.annotations)
- **新**: SpringDoc OpenAPI 3 (io.swagger.v3.oas.annotations)

#### 3.2 JSON 处理
- **旧**: Fastjson (com.alibaba.fastjson)
- **新**: Jackson (com.fasterxml.jackson)

#### 3.3 数据库访问
- **旧**: MyBatis-Plus (依赖公司基础库)
- **新**: 内存存储 (ConcurrentHashMap) - 可轻松替换为 JPA/JdbcTemplate

### 4. 保留的核心逻辑

#### 4.1 Nacos 服务查询
```java
// 保留功能
- getAllInstances(String serviceName)
- getAllInstancesPage(InstanceQuery query)
- updateInstanceWeight(InstanceDTO dto)
- updateInstanceEnabled(InstanceDTO dto)
```

#### 4.2 路由 CRUD
```java
// 保留功能
- addRoute(RouteDTO routeDTO)
- updateRoute(RouteDTO routeDTO)
- deleteRoute(Long id)
- getById(String id)
- routeList()
- selectRoutePageVo(RouteQuery query)
```

#### 4.3 Redis 操作
```java
// 保留功能
- reloadConfig() // 发布路由更新消息到 Redis
```

### 5. 配置文件变更

#### 5.1 application.yml
```yaml
# 新增配置
jwt:
  secret: ${JWT_SECRET:lite-gateway-secret-key-for-jwt-signing}
  expiration: ${JWT_EXPIRATION:86400000}

# Redis 配置更新为 Spring Boot 3.x 格式
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

### 6. API 路径变更

| 旧路径 | 新路径 |
|--------|--------|
| `/gateway/route/selectRoutePageVo` | `/gateway/route/page` |
| `/gateway/route/getOne` | `/gateway/route/{id}` |
| `/gateway/route/addRoute` | `/gateway/route` (POST) |
| `/gateway/route/updateRoute` | `/gateway/route/{id}` (PUT) |
| `/gateway/route/updateRouteStatus` | `/gateway/route/{id}/status` (PATCH) |
| `/gateway/route/deleteRoute` | `/gateway/route/{id}` (DELETE) |
| `/gateway/route/reloadConfig` | `/gateway/route/reload` (POST) |
| `/gateway/route/getAllInterface` | `/gateway/route/{id}/interfaces` |
| `/gateway/route/getAllInstances` | `/gateway/route/instances` |
| `/gateway/route/getAllInstancesPage` | `/gateway/route/instances/page` |
| `/gateway/route/updateInstanceWeight` | `/gateway/route/instances/weight` |
| `/gateway/route/updateInstanceEnabled` | `/gateway/route/instances/enabled` |

### 7. 文件结构

```
lite-gateway-admin/
├── src/main/java/com/litegateway/admin/
│   ├── LiteGatewayAdminApplication.java
│   ├── common/
│   │   ├── ErrorCodeEnum.java
│   │   ├── exception/
│   │   │   ├── BizException.java
│   │   │   ├── BizExceptionAssert.java
│   │   │   └── ErrorCode.java
│   │   └── web/
│   │       ├── PageBody.java
│   │       └── Result.java
│   ├── config/
│   │   └── AppConfig.java
│   ├── constants/
│   │   ├── RedisTypeConstants.java
│   │   └── StringConstants.java
│   ├── controller/
│   │   └── GatewayRouteController.java
│   ├── dto/
│   │   ├── InstanceDTO.java
│   │   ├── InterfaceDTO.java
│   │   └── RouteDTO.java
│   ├── query/
│   │   ├── InstanceQuery.java
│   │   ├── PageQuery.java
│   │   └── RouteQuery.java
│   ├── service/
│   │   ├── GatewayRouteService.java
│   │   └── impl/
│   │       └── GatewayRouteServiceImpl.java
│   ├── utils/
│   │   └── JwtUtils.java
│   └── vo/
│       └── RouteVO.java
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

## 后续步骤

1. **添加数据库支持**: 当前使用内存存储，可根据需要添加 JPA/MyBatis 支持
2. **添加安全认证**: 可集成 Spring Security + JWT 实现完整认证流程
3. **添加单元测试**: 为 Service 和 Controller 添加测试用例
4. **配置 CI/CD**: 添加 Dockerfile 和 Jenkins/GitHub Actions 配置

## 注意事项

1. 当前实现使用内存存储路由数据，重启后数据会丢失
2. JWT Secret 应通过环境变量配置，不要使用默认值
3. Nacos 和 Redis 连接信息需要通过环境变量配置
