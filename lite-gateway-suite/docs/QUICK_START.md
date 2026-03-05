# Lite Gateway 快速启动指南

> 多种启动方式，满足不同场景需求

## 📋 启动方式一览

| 方式 | 命令 | 适用场景 | 包含服务 |
|------|------|----------|----------|
| **仅 Redis** | `docker-compose -f docker-compose.redis.yml up -d` | 只需要缓存服务 | Redis |
| **仅 Nacos** | `docker-compose -f docker-compose.nacos.yml up -d` | 只需要注册中心 | Nacos |
| **中间件全套** | `./scripts/start-all-middleware.sh` | 开发环境，需要基础设施 | Redis + Nacos |
| **嵌入式** | `docker-compose -f docker-compose.embedded.yml up -d` | 完整开发环境 | MySQL + Redis + Nacos + 应用 |
| **完整版** | `docker-compose up -d` | 生产环境模拟 | 所有服务 + 监控 |

---

## 1. 单独启动 Redis

### 1.1 适用场景
- ✅ 已有 Nacos，只需要 Redis
- ✅ 测试缓存功能
- ✅ 资源极其有限

### 1.2 启动命令

```bash
# Linux/Mac
chmod +x scripts/start-redis.sh
./scripts/start-redis.sh

# Windows
scripts\start-redis.bat

# 或直接使用 Docker Compose
docker-compose -f docker-compose.redis.yml up -d
```

### 1.3 连接信息

```
🌐 主机: localhost
🔌 端口: 6379
🔑 密码: 无
🔗 连接: redis://localhost:6379
```

### 1.4 常用命令

```bash
# 进入 Redis 容器
docker exec -it lite-gateway-redis redis-cli

# 查看日志
docker logs -f lite-gateway-redis

# 停止服务
docker-compose -f docker-compose.redis.yml down
```

---

## 2. 单独启动 Nacos

### 2.1 适用场景
- ✅ 已有 Redis，只需要 Nacos
- ✅ 测试服务注册发现
- ✅ 与其他项目共用 Nacos

### 2.2 启动命令

```bash
# Linux/Mac
chmod +x scripts/start-nacos.sh
./scripts/start-nacos.sh

# Windows
scripts\start-nacos.bat

# 或直接使用 Docker Compose
docker-compose -f docker-compose.nacos.yml up -d
```

### 2.3 访问信息

```
🌐 控制台: http://localhost:8848/nacos
👤 账号: nacos
🔑 密码: nacos
```

### 2.4 常用命令

```bash
# 查看日志
docker logs -f lite-gateway-nacos

# 停止服务
docker-compose -f docker-compose.nacos.yml down
```

---

## 3. 一键启动所有中间件（推荐开发使用）

### 3.1 适用场景
- ✅ 从零开始开发
- ✅ 需要 Redis + Nacos
- ✅ 在 IDE 中启动应用服务

### 3.2 启动命令

```bash
# Linux/Mac
chmod +x scripts/start-all-middleware.sh
./scripts/start-all-middleware.sh

# Windows
scripts\start-all-middleware.bat

# 或手动启动
docker-compose -f docker-compose.redis.yml up -d
docker-compose -f docker-compose.nacos.yml up -d
```

### 3.3 连接信息

```
📍 Redis:
   主机: localhost
   端口: 6379
   连接: redis://localhost:6379

📍 Nacos:
   控制台: http://localhost:8848/nacos
   账号: nacos
   密码: nacos
```

### 3.4 开发流程

```bash
# 1. 启动中间件
./scripts/start-all-middleware.sh

# 2. 在 IDE 中启动 Admin 服务
#    - 设置 profile: dev
#    - 配置: spring.redis.host=localhost
#    - 配置: spring.cloud.nacos.discovery.server-addr=localhost:8848

# 3. 在 IDE 中启动 Core 服务
#    - 设置 profile: dev
#    - 配置: spring.redis.host=localhost
#    - 配置: spring.cloud.nacos.discovery.server-addr=localhost:8848

# 4. 开发调试...

# 5. 停止中间件
docker-compose -f docker-compose.redis.yml down
docker-compose -f docker-compose.nacos.yml down
```

---

## 4. 嵌入式完整环境（一键开发）

### 4.1 适用场景
- ✅ 没有现有基础设施
- ✅ 需要完整测试环境
- ✅ 快速演示

### 4.2 启动命令

```bash
# Linux/Mac
chmod +x scripts/start-embedded.sh
./scripts/start-embedded.sh

# Windows
scripts\start-embedded.bat

# 或直接使用 Docker Compose
docker-compose -f docker-compose.embedded.yml up -d
```

### 4.3 包含服务

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | root/123456 |
| Redis | 6379 | 无密码 |
| Nacos | 8848 | nacos/nacos |
| Admin | 8080 | 管理后台 API |
| Gateway | 8088 | 网关服务 |
| UI | 8000 | 前端控制台 |
| User Service | 9001 | 测试服务 |
| Order Service | 9002 | 测试服务 |

---

## 5. 完整生产环境（含监控）

### 5.1 适用场景
- ✅ 生产环境部署
- ✅ 需要监控大盘
- ✅ 链路追踪分析

### 5.2 启动命令

```bash
# 启动可观测性组件（Jaeger + Prometheus + Grafana）
docker-compose -f docker-compose.observability.yml up -d

# 启动核心业务
docker-compose up -d
```

### 5.3 监控地址

```
📊 Jaeger UI:   http://localhost:16686
📈 Prometheus:  http://localhost:9090
📉 Grafana:     http://localhost:3000 (admin/admin)
```

---

## 6. 方案对比

| 方案 | 内存占用 | 启动时间 | 适用场景 |
|------|----------|----------|----------|
| 仅 Redis | ~50MB | 3秒 | 只需要缓存 |
| 仅 Nacos | ~512MB | 15秒 | 只需要注册中心 |
| 中间件全套 | ~600MB | 20秒 | **推荐开发** |
| 嵌入式完整 | ~2GB | 60秒 | 完整测试 |
| 生产完整 | ~4GB | 120秒 | 生产模拟 |

---

## 7. 常见问题

### Q1: 端口被占用怎么办？

```bash
# 修改 docker-compose.redis.yml
services:
  redis:
    ports:
      - "6380:6379"  # 使用 6380 代替 6379

# 修改 docker-compose.nacos.yml
services:
  nacos:
    ports:
      - "8849:8848"  # 使用 8849 代替 8848
```

### Q2: 如何查看日志？

```bash
# 查看 Redis 日志
docker logs -f lite-gateway-redis

# 查看 Nacos 日志
docker logs -f lite-gateway-nacos

# 查看所有日志
docker-compose -f docker-compose.redis.yml logs -f
docker-compose -f docker-compose.nacos.yml logs -f
```

### Q3: 如何重启单个服务？

```bash
docker-compose -f docker-compose.redis.yml restart
docker-compose -f docker-compose.nacos.yml restart
```

### Q4: 如何完全清理环境？

```bash
# 停止并删除容器
docker-compose -f docker-compose.redis.yml down
docker-compose -f docker-compose.nacos.yml down

# 停止并删除容器 + 数据卷（谨慎使用）
docker-compose -f docker-compose.redis.yml down -v
docker-compose -f docker-compose.nacos.yml down -v
```

### Q5: 数据存储在哪里？

```bash
# 查看数据卷
docker volume ls | grep lite-gateway

# 查看具体位置
docker volume inspect lite-gateway-suite_redis_data
docker volume inspect lite-gateway-suite_nacos_data
```

---

## 8. 快速测试

### 8.1 测试 Redis

```bash
# 进入 Redis 容器
docker exec -it lite-gateway-redis redis-cli

# 测试命令
127.0.0.1:6379> ping
PONG
127.0.0.1:6379> set test "hello"
OK
127.0.0.1:6379> get test
"hello"
```

### 8.2 测试 Nacos

```bash
# 注册一个测试服务
curl -X POST 'http://localhost:8848/nacos/v1/ns/instance' \
  -d 'serviceName=test-service' \
  -d 'ip=127.0.0.1' \
  -d 'port=8080'

# 查询服务列表
curl 'http://localhost:8848/nacos/v1/ns/instance/list?serviceName=test-service'
```

---

**推荐开发流程**：
1. 启动中间件：`./scripts/start-all-middleware.sh`
2. 在 IDE 中启动 Admin 和 Core 服务
3. 开发调试
4. 需要测试完整链路时，再启动 `docker-compose.embedded.yml`
