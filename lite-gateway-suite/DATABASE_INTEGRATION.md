# Lite Gateway 数据库集成完成

## 概述
已成功为 lite-gateway-admin 和 lite-gateway-core 两个模块添加了 MyBatis-Plus + MySQL 数据库支持。

## 已完成的工作

### 1. 依赖添加 ✅
两个模块都添加了以下依赖：
- `mybatis-plus-boot-starter` (3.5.5)
- `mysql-connector-j` (8.0.33)
- `druid-spring-boot-3-starter` (1.2.20)
- `flyway-mysql` (数据库迁移)

### 2. 数据库表结构 ✅

#### gateway_route - 网关路由表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键ID |
| route_id | varchar | 路由唯一标识 |
| name | varchar | 路由名称 |
| uri | varchar | 目标URI |
| path | varchar | 路径断言 |
| strip_prefix | int | 路径截取前缀数 |
| host | varchar | 主机断言 |
| remote_addr | varchar | 远程地址断言 |
| header | varchar | Header断言 |
| filter_rate_limiter_name | varchar | 限流器名称 |
| replenish_rate | int | 每秒补充令牌数 |
| burst_capacity | int | 令牌桶容量 |
| weight | int | 权重 |
| weight_name | varchar | 权重分组名 |
| status | tinyint | 状态：0启用 1禁用 |
| description | varchar | 描述 |
| deleted | tinyint | 逻辑删除 |

#### ip_blacklist - IP黑名单表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键ID |
| ip | varchar | IP地址 |
| remark | varchar | 备注 |
| deleted | tinyint | 逻辑删除 |

#### white_list - 白名单表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键ID |
| path | varchar | 路径 |
| description | varchar | 描述 |
| deleted | tinyint | 逻辑删除 |

#### sys_user - 系统用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键ID |
| username | varchar | 用户名 |
| password | varchar | 密码(BCrypt加密) |
| nickname | varchar | 昵称 |
| email | varchar | 邮箱 |
| phone | varchar | 手机号 |
| avatar | varchar | 头像 |
| status | tinyint | 状态 |
| deleted | tinyint | 逻辑删除 |

### 3. 实体类 (Entity) ✅

**lite-gateway-admin 模块:**
- `GatewayRoute.java`
- `IpBlacklist.java`
- `WhiteList.java`
- `SysUser.java`

**lite-gateway-core 模块:**
- `GatewayRoute.java`
- `IpBlacklist.java`
- `WhiteList.java`

### 4. Mapper 接口 ✅

**lite-gateway-admin:**
- `GatewayRouteMapper.java` - 包含 `selectEnabledRoutes()` 方法
- `IpBlacklistMapper.java`
- `WhiteListMapper.java`
- `SysUserMapper.java` - 包含 `selectByUsername()` 方法

**lite-gateway-core:**
- `GatewayRouteMapper.java`
- `IpBlacklistMapper.java`
- `WhiteListMapper.java`

### 5. Service 层更新 ✅

**GatewayRouteServiceImpl.java** 已更新：
- 继承 `ServiceImpl<GatewayRouteMapper, GatewayRoute>`
- 使用 MyBatis-Plus 进行 CRUD 操作
- 使用 LambdaQueryWrapper 进行条件查询
- 使用 Page 进行分页查询

### 6. 数据同步服务 ✅

**lite-gateway-core 新增:**
- `DataSyncService.java` - 启动时从数据库加载数据
- 同步路由、IP黑名单、白名单到缓存
- 监听 Redis 消息触发同步

### 7. 配置文件更新 ✅

**application.yml 新增配置:**
```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:lite_gateway}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:}
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### 8. Flyway 数据库迁移 ✅

**迁移脚本位置:**
- `src/main/resources/db/migration/V1__init_schema.sql`

**包含内容:**
- 创建所有表结构
- 添加索引
- 插入默认管理员用户 (admin/admin123)

## 环境变量配置

启动前需要配置以下环境变量：

```bash
# MySQL 配置
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DB=lite_gateway
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# Nacos 配置
export NACOS_SERVER=localhost:8848

# JWT 配置
export JWT_SECRET=your-secret-key
export JWT_EXPIRATION=86400000
```

## 使用步骤

1. **创建数据库:**
```sql
CREATE DATABASE lite_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **启动应用:**
应用会自动执行 Flyway 迁移脚本创建表结构

3. **默认账号:**
- 用户名: admin
- 密码: admin123

## 架构说明

```
┌─────────────────────────────────────────────────────────────┐
│                    lite-gateway-admin                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Controller │→ │   Service    │→ │    Mapper    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                 ↓                 ↓               │
│    HTTP Request    Business Logic      MyBatis-Plus         │
│         ↓                 ↓                 ↓               │
│  ┌──────────────────────────────────────────────────┐      │
│  │                    MySQL                         │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Redis Pub/Sub
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    lite-gateway-core                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    Filter    │  │ DataSyncSvc  │→ │    Mapper    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                 ↓                 ↓               │
│    Gateway Filter   Sync from DB      MyBatis-Plus          │
│         ↓                 ↓                                 │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │    Cache     │  │    MySQL     │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

## 注意事项

1. **逻辑删除**: 所有表都使用逻辑删除（deleted字段），不会真正删除数据
2. **密码加密**: 用户密码使用 BCrypt 加密存储
3. **连接池**: 使用 Druid 连接池，支持监控
4. **分页**: 使用 MyBatis-Plus 分页插件
5. **环境变量**: 敏感配置通过环境变量传入，不硬编码

## 后续优化建议

1. 添加数据库连接池监控页面
2. 实现读写分离（如果需要）
3. 添加数据库性能监控
4. 实现分库分表（数据量大时）
