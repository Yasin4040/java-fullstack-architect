# Lite Gateway 架构设计

## 架构演进

### 原架构（Core 直连数据库）
```
┌─────────────────────────────────────────────────────────────┐
│                    lite-gateway-admin                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Controller │→ │   Service    │→ │    MySQL     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Redis Pub/Sub
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    lite-gateway-core                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    Filter    │  │    Cache     │→ │    MySQL     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

**问题：**
- Core 模块需要数据库连接，增加了复杂度
- 数据库连接数增加（Admin + Core）
- 违反了"网关只负责转发"的单一职责原则

---

### 新架构（Core 通过 Admin API 获取配置）
```
┌─────────────────────────────────────────────────────────────┐
│                    lite-gateway-admin                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Controller │→ │   Service    │→ │    MySQL     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                                                    │
│    HTTP API (/api/config/*)                                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP 轮询 / Redis 通知
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    lite-gateway-core                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    Filter    │  │ ConfigClient │  │ Local Cache  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                 ↓                                  │
│    Gateway Filter   从 Admin 拉取配置                        │
│         ↓                 ↓                                  │
│  ┌──────────────────────────────────────┐                  │
│  │         无数据库连接                  │                  │
│  └──────────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
```

**优点：**
- Core 保持轻量，只专注网关转发
- 数据库连接只在 Admin，便于管理
- 符合微服务"数据私有"原则
- 可以添加缓存层，减少数据库压力

---

## 配置同步机制

### 1. 启动时同步
Core 启动时立即调用 Admin API 获取完整配置：
```java
@PostConstruct
public void init() {
    syncConfig();  // 立即同步一次
}
```

### 2. 定时轮询（默认30秒）
```java
@Scheduled(fixedRate = 30000)
public void scheduledSync() {
    checkAndSync();  // 检查版本号，有更新则同步
}
```

### 3. Redis 通知（实时）
Admin 配置变更时：
1. 增加配置版本号
2. 发布 Redis 消息
3. Core 收到消息后立即同步

---

## Admin API 接口

### 获取完整配置
```
GET /api/config/gateway
Response: {
    "code": "00000",
    "data": {
        "version": 1234567890,
        "routes": [...],
        "ipBlacklist": [...],
        "whiteList": [...]
    }
}
```

### 获取配置版本号
```
GET /api/config/version
Response: {
    "code": "00000",
    "data": 1234567890
}
```

### 检查配置更新
```
GET /api/config/check?clientVersion=1234567890
Response: {
    "code": "00000",
    "data": null  // 无更新时返回 null
}
```

---

## 配置版本管理

### 版本号生成
使用原子递增的 Long 类型：
```java
private final AtomicLong configVersion = new AtomicLong(System.currentTimeMillis());
```

### 版本号更新时机
- 新增路由
- 修改路由
- 删除路由
- 修改 IP 黑名单
- 修改白名单

---

## 环境变量配置

### Admin 模块
```bash
# MySQL
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DB=lite_gateway
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### Core 模块
```bash
# Admin 服务地址
export ADMIN_URL=http://localhost:8080

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

---

## 文件结构

```
lite-gateway-suite/
├── lite-gateway-admin/          # 管理后台（连数据库）
│   ├── controller/
│   │   ├── GatewayRouteController.java
│   │   └── ConfigController.java      # 新增：配置查询接口
│   ├── service/
│   │   ├── GatewayRouteService.java
│   │   ├── ConfigService.java         # 新增：配置服务
│   │   └── impl/
│   ├── repository/
│   │   ├── entity/                    # 数据库实体
│   │   └── mapper/                    # MyBatis Mapper
│   └── resources/
│       └── db/migration/              # Flyway 迁移脚本
│
└── lite-gateway-core/           # 网关核心（无数据库）
    ├── client/
    │   └── AdminConfigClient.java     # 新增：Admin 客户端
    ├── service/
    │   └── ConfigSyncService.java     # 新增：配置同步服务
    ├── cache/
    │   ├── IpListCache.java
    │   └── WhiteListCache.java
    └── config/
        └── AdminClientConfig.java     # 新增：Admin 客户端配置
```

---

## 优势总结

1. **职责分离**: Admin 负责数据管理，Core 负责网关转发
2. **简化部署**: Core 无需数据库连接配置
3. **易于扩展**: 可以部署多个 Core 实例，共用一个 Admin
4. **数据安全**: 数据库不直接暴露给网关层
5. **灵活更新**: 支持定时轮询 + Redis 通知两种更新方式
