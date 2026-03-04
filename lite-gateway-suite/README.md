# Lite Gateway Suite

> 轻量级微服务网关 - 填补 Nginx 配置繁琐与 Shenyu/Kong 过于复杂之间的空白

## 🎯 项目简介

一个**轻量级、易部署、带可视化界面**的微服务网关，让中小团队能在 **5 分钟内** 拥有动态路由管理能力。

**核心卖点**：一个 Redis + 一个 Jar，填表单即可配置转发，自动对接 Nacos 服务发现。

## 🏗️ 技术架构

```
lite-gateway-suite/
├── lite-gateway-admin/    # 管理端：Spring Boot 3.x (Port 8080)
├── lite-gateway-core/     # 网关：Spring Cloud Gateway 4.x (Port 80)
└── lite-gateway-ui/       # 前端：Vue3 + Vite + Ant Design Vue (Port 3000)
```

**基础设施**：Redis（配置存储 + 通知）、Nacos（可选，服务发现）

## 🚀 快速开始

### 方式一：Docker Compose 一键启动

```bash
# 克隆项目
git clone https://github.com/your-repo/lite-gateway-suite.git
cd lite-gateway-suite

# 一键启动所有服务
make up

# 或者使用 docker-compose
docker-compose up -d
```

访问地址：
- 管理后台 UI：`http://localhost:8000`
- 管理后台 API：`http://localhost:8080`
- 网关服务：`http://localhost`

### 方式二：本地开发

```bash
# 1. 启动基础设施（Redis + Nacos）
make dev-infra

# 2. 启动 Admin 服务（新终端）
make run-admin

# 3. 启动 Gateway 服务（新终端）
make run-gateway

# 4. 启动 UI 开发服务器（新终端）
make dev-ui
```

## ✅ 功能特性

### 已实现

- [x] 路由 CRUD 管理（Static / Nacos 双类型）
- [x] 动态路由刷新（Redis 订阅通知，1秒内生效）
- [x] JWT 认证过滤器
- [x] 简单计数限流
- [x] Nacos 服务发现对接
- [x] 可视化配置界面

### 规划中

- [ ] 熔断降级
- [ ] 灰度发布
- [ ] 访问日志分析
- [ ] Prometheus 监控
- [ ] 插件扩展机制

## 📁 项目结构

```
lite-gateway-suite/
├── docker-compose.yml           # 一键启动配置
├── Makefile                     # 快捷命令
├── README.md                    # 项目说明
│
├── lite-gateway-admin/          # 管理端服务
│   ├── src/main/java/
│   │   ├── controller/          # 控制器
│   │   ├── service/             # 业务逻辑
│   │   └── config/              # 配置类
│   ├── src/main/resources/
│   ├── pom.xml
│   └── Dockerfile
│
├── lite-gateway-core/           # 网关服务
│   ├── src/main/java/
│   │   ├── repository/          # 路由存储
│   │   ├── resolver/            # 目标解析器
│   │   ├── filter/              # 过滤器
│   │   └── listener/            # 配置监听
│   ├── src/main/resources/
│   ├── pom.xml
│   └── Dockerfile
│
└── lite-gateway-ui/             # 前端界面
    ├── src/
    │   ├── api/                 # API 封装
    │   ├── views/               # 页面视图
    │   └── main.ts
    ├── package.json
    └── vite.config.ts
```

## 🔌 核心流程

### 动态路由更新

```
1. 用户在 UI 新增/修改路由
        ↓
2. Admin 服务保存到 Redis
        ↓
3. Admin 发布通知到 Redis Channel
        ↓
4. Gateway 监听 Channel 收到通知
        ↓
5. Gateway 从 Redis 读取最新配置
        ↓
6. Gateway 刷新路由表（无需重启）
        ↓
7. 新路由生效
```

## 🛠️ 常用命令

```bash
# 启动所有服务
make up

# 停止所有服务
make down

# 查看日志
make logs

# 只启动基础设施（开发模式）
make dev-infra

# 打包所有服务
make package

# 清理所有容器和卷
make clean
```

## 📝 配置说明

### Admin 服务配置

```yaml
# application.yml
server:
  port: 8080

spring:
  redis:
    host: localhost
    port: 6379
    database: 0

# JWT 配置
jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时
```

### Gateway 服务配置

```yaml
# application.yml
server:
  port: 80

spring:
  redis:
    host: localhost
    port: 6379
    database: 0

# 网关配置从 Redis 读取，无需静态配置
```

## 🤝 贡献指南

欢迎提交 Issue 和 PR！

## 📄 许可证

MIT License
