# Nacos 服务集成实战指南

> 基于 Lite Gateway Suite 项目的 Nacos 服务发现与配置中心完整实践

## 📚 目录

1. [Nacos 简介](#1-nacos-简介)
2. [架构设计](#2-架构设计)
3. [环境搭建](#3-环境搭建)
4. [服务注册与发现](#4-服务注册与发现)
5. [配置中心](#5-配置中心)
6. [网关集成](#6-网关集成)
7. [实战演练](#7-实战演练)
8. [常见问题](#8-常见问题)

---

## 1. Nacos 简介

### 1.1 什么是 Nacos

Nacos（Dynamic Naming and Configuration Service）是阿里巴巴开源的一个更易于构建云原生应用的动态服务发现、配置管理和服务管理平台。

### 1.2 核心功能

| 功能 | 说明 | 适用场景 |
|------|------|----------|
| **服务发现** | 服务注册与发现，健康检查 | 微服务架构中服务间调用 |
| **配置管理** | 动态配置推送，版本管理 | 配置热更新，无需重启 |
| **服务管理** | 服务元数据管理，权重配置 | 灰度发布，流量控制 |

### 1.3 与当前项目的关系

```
┌─────────────────────────────────────────────────────────────┐
│                    Nacos Server (8848)                       │
│  ┌─────────────────────┐  ┌─────────────────────┐          │
│  │   服务注册中心       │  │    配置中心          │          │
│  │  - user-service     │  │  - 路由配置          │          │
│  │  - order-service    │  │  - 限流规则          │          │
│  │  - gateway-core     │  │  - 黑名单配置        │          │
│  └─────────────────────┘  └─────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              ↑
                              │ 注册 & 发现
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ user-service  │    │ order-service │    │gateway-core   │
│   (9001)      │    │   (9002)      │    │  (8088)       │
└───────────────┘    └───────────────┘    └───────────────┘
        ↑                     ↑                     ↑
        └─────────────────────┴─────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │  lite-gateway-ui  │
                    │    (8000)         │
                    └───────────────────┘
```

---

## 2. 架构设计

### 2.1 当前项目架构

```
lite-gateway-suite/
├── lite-gateway-admin/      # 管理后台 (8080)
│   └── 管理路由配置、连MySQL
├── lite-gateway-core/       # 网关核心 (8088)
│   └── 从Redis/Admin获取配置，支持Nacos服务发现
├── lite-gateway-ui/         # 前端界面 (8000)
├── test-services/
│   ├── user-service/        # 用户服务 (9001) - 已集成Nacos
│   └── order-service/       # 订单服务 (9002) - 已集成Nacos
└── Nacos Server/            # 服务注册中心 (8848)
```

### 2.2 数据流向

```
1. 服务启动
   user-service/order-service → 向 Nacos 注册服务

2. 网关路由配置
   Admin 配置 Nacos 类型路由 → 保存到 MySQL → 同步到 Redis

3. 请求转发
   客户端 → Gateway → 从 Nacos 发现服务实例 → 负载均衡转发

4. 配置更新
   Nacos 配置变更 → 监听推送 → 服务热更新
```

---

## 3. 环境搭建

### 3.1 启动 Nacos（Docker 方式）

#### 方式一：使用项目内置 Docker Compose

```bash
# 在项目根目录执行，会自动启动 Nacos
docker-compose up -d nacos

# 查看 Nacos 日志
docker logs -f lite-gateway-nacos
```

#### 方式二：独立启动 Nacos

```bash
# 拉取镜像
docker pull nacos/nacos-server:v2.2.3

# 单机模式启动
docker run -d \
  --name nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e PREFER_HOST_MODE=hostname \
  nacos/nacos-server:v2.2.3
```

#### 方式三：本地安装（开发环境）

```bash
# 1. 下载 Nacos
cd /opt
wget https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.tar.gz
tar -zxvf nacos-server-2.2.3.tar.gz
cd nacos

# 2. 单机模式启动
sh bin/startup.sh -m standalone

# Windows
startup.cmd -m standalone
```

### 3.2 验证 Nacos 启动

```bash
# 访问控制台
curl http://localhost:8848/nacos

# 默认账号密码
用户名：nacos
密码：nacos
```

### 3.3 Nacos 控制台介绍

```
┌─────────────────────────────────────────────────────────────┐
│  Nacos 控制台                                                │
├──────────────┬──────────────────────────────────────────────┤
│              │                                              │
│  配置管理     │  • 配置列表 - 查看所有配置                    │
│  ├── 配置列表 │  • 历史版本 - 配置变更历史                    │
│  ├── 历史版本 │  • 监听查询 - 查看配置监听者                  │
│              │                                              │
│  服务管理     │  • 服务列表 - 查看注册的服务                  │
│  ├── 服务列表 │  • 订阅者列表 - 查看服务消费者                │
│  └── 订阅者列表│                                              │
│              │                                              │
│  权限控制     │  • 用户管理                                  │
│  └── 用户列表 │  • 角色管理                                  │
│              │  • 权限管理                                  │
└──────────────┴──────────────────────────────────────────────┘
```

---

## 4. 服务注册与发现

### 4.1 测试服务已集成的 Nacos 配置

user-service 和 order-service 已集成 Nacos，配置如下：

**user-service/src/main/resources/application.yml**
```yaml
spring:
  application:
    name: user-service  # 服务名，注册到 Nacos

  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        enabled: ${NACOS_ENABLED:true}
```

### 4.2 启动测试服务并注册到 Nacos

#### Docker 方式（推荐）

```bash
# 在项目根目录执行
docker-compose up -d user-service order-service

# 查看服务日志
docker logs -f user-service
docker logs -f order-service
```

#### 本地 Maven 启动

```bash
# 启动用户服务
cd test-services/user-service
mvn clean package -DskipTests
java -jar target/user-service-1.0.0-SNAPSHOT.jar \
  --NACOS_ENABLED=true \
  --NACOS_SERVER=localhost:8848

# 启动订单服务
cd test-services/order-service
mvn clean package -DskipTests
java -jar target/order-service-1.0.0-SNAPSHOT.jar \
  --NACOS_ENABLED=true \
  --NACOS_SERVER=localhost:8848
```

### 4.3 验证服务注册

```bash
# 方式一：Nacos 控制台查看
# 访问 http://localhost:8848/nacos → 服务管理 → 服务列表

# 方式二：API 查询
curl http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10

# 方式三：查看服务详情
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service
```

### 4.4 服务发现原理

```java
// 服务注册流程
@Service
public class NacosDiscoveryExample {
    
    @Autowired
    private NacosDiscoveryProperties discoveryProperties;
    
    @Autowired
    private NamingService namingService;
    
    // 1. 服务注册（自动完成）
    public void registerInstance() throws NacosException {
        Instance instance = new Instance();
        instance.setIp("192.168.1.100");
        instance.setPort(9001);
        instance.setServiceName("user-service");
        instance.setHealthy(true);
        instance.setWeight(1.0);
        
        namingService.registerInstance("user-service", instance);
    }
    
    // 2. 服务发现
    public List<Instance> discoverInstances() throws NacosException {
        // 获取所有健康实例
        return namingService.selectInstances("user-service", true);
    }
    
    // 3. 服务订阅（监听变化）
    public void subscribeService() throws NacosException {
        namingService.subscribe("user-service", event -> {
            System.out.println("服务实例发生变化: " + event);
        });
    }
}
```

---

## 5. 配置中心

### 5.1 创建配置

#### 方式一：Nacos 控制台

```
1. 登录 http://localhost:8848/nacos
2. 配置管理 → 配置列表
3. 点击 "+" 新建配置

配置内容示例：
Data ID: user-service-dev.yaml
Group: DEFAULT_GROUP
配置格式: YAML

配置内容：
server:
  port: 9001
user:
  name: 测试用户
  max-age: 100
```

#### 方式二：Open API

```bash
# 发布配置
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=user-service-dev.yaml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=server.port=9001"

# 获取配置
curl "http://localhost:8848/nacos/v1/cs/configs?dataId=user-service-dev.yaml&group=DEFAULT_GROUP"

# 删除配置
curl -X DELETE "http://localhost:8848/nacos/v1/cs/configs?dataId=user-service-dev.yaml&group=DEFAULT_GROUP"
```

### 5.2 服务集成配置中心

**添加依赖**
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    <version>2022.0.0.0</version>
</dependency>
```

**bootstrap.yml**
```yaml
spring:
  application:
    name: user-service
  profiles:
    active: dev
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        file-extension: yaml
        namespace: 
        group: DEFAULT_GROUP
```

**动态配置刷新**
```java
@RestController
@RefreshScope  // 配置变更自动刷新
public class ConfigController {
    
    @Value("${user.name:default}")
    private String userName;
    
    @Value("${user.max-age:0}")
    private Integer maxAge;
    
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("userName", userName);
        config.put("maxAge", maxAge);
        return config;
    }
}
```

---

## 6. 网关集成

### 6.1 网关 Nacos 配置

**lite-gateway-core/src/main/resources/application.yml**
```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: true  # 启用 Nacos 服务发现
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
```

### 6.2 配置 Nacos 类型路由

#### 在 Admin 后台配置

```
登录 http://localhost:8000

1. 路由管理 → 新增路由

路由ID: user-service-nacos
路由类型: Nacos
路径: /api/users/**
服务名: user-service
优先级: 100

路由ID: order-service-nacos
路由类型: Nacos
路径: /api/orders/**
服务名: order-service
优先级: 100
```

#### 直接操作数据库

```sql
-- 插入 Nacos 类型路由
INSERT INTO gateway_route (
    route_id, 
    route_type, 
    route_path, 
    service_name,
    status,
    priority,
    create_time,
    update_time
) VALUES 
('user-service-nacos', 'NACOS', '/api/users/**', 'user-service', 1, 100, NOW(), NOW()),
('order-service-nacos', 'NACOS', '/api/orders/**', 'order-service', 1, 100, NOW(), NOW());
```

### 6.3 负载均衡配置

```yaml
spring:
  cloud:
    loadbalancer:
      nacos:
        enabled: true
      configurations: default
      health-check:
        interval: 5000
        refetch-instances-interval: 10000
```

### 6.4 服务发现流程

```
1. 请求到达网关
   GET http://localhost:8088/api/users/list

2. 网关匹配路由
   路径 /api/users/** 匹配到 user-service-nacos

3. 从 Nacos 获取服务实例
   NacosNamingService.selectInstances("user-service", true)
   
4. 负载均衡选择实例
   - instance-1: 192.168.1.100:9001 (weight: 1.0)
   - instance-2: 192.168.1.101:9001 (weight: 1.0)
   
5. 转发请求
   GET http://192.168.1.100:9001/api/users/list
```

---

## 7. 实战演练

### 7.1 完整启动流程

```bash
# 步骤 1: 启动基础设施
docker-compose up -d redis nacos

# 等待 Nacos 启动完成（约 30 秒）
sleep 30

# 步骤 2: 启动 Admin 服务
docker-compose up -d lite-gateway-admin

# 步骤 3: 启动测试服务（自动注册到 Nacos）
docker-compose up -d user-service order-service

# 步骤 4: 启动网关
docker-compose up -d lite-gateway-core

# 步骤 5: 启动前端
docker-compose up -d lite-gateway-ui

# 查看所有服务状态
docker-compose ps
```

### 7.2 验证服务注册

```bash
# 查看 Nacos 服务列表
curl http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10

# 预期输出包含：
# - user-service
# - order-service
```

### 7.3 配置网关路由

```bash
# 方式一：通过 Admin API 配置

# 1. 登录获取 Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | jq -r '.data.token')

# 2. 创建用户服务路由
curl -X POST http://localhost:8080/api/routes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "routeId": "user-service-nacos",
    "routeType": "NACOS",
    "routePath": "/api/users/**",
    "serviceName": "user-service",
    "status": 1,
    "priority": 100
  }'

# 3. 创建订单服务路由
curl -X POST http://localhost:8080/api/routes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "routeId": "order-service-nacos",
    "routeType": "NACOS",
    "routePath": "/api/orders/**",
    "serviceName": "order-service",
    "status": 1,
    "priority": 100
  }'
```

### 7.4 测试网关转发

```bash
# 测试用户服务（通过网关）
curl http://localhost:8088/api/users/health

# 预期输出：
# {
#   "service": "user-service",
#   "port": "9001",
#   "status": "UP",
#   "timestamp": "2024-..."
# }

# 测试订单服务（通过网关）
curl http://localhost:8088/api/orders/health

# 测试用户列表
curl http://localhost:8088/api/users

# 测试订单列表
curl http://localhost:8088/api/orders
```

### 7.5 服务扩缩容测试

```bash
# 扩容用户服务到 3 个实例
docker-compose up -d --scale user-service=3

# 查看 Nacos 中的实例数
# 访问 http://localhost:8848/nacos → 服务管理 → user-service

# 多次请求，观察负载均衡
curl http://localhost:8088/api/users/health
curl http://localhost:8088/api/users/health
curl http://localhost:8088/api/users/health

# 缩容回 1 个实例
docker-compose up -d --scale user-service=1
```

### 7.6 服务下线测试

```bash
# 停止用户服务
docker-compose stop user-service

# 等待 5 秒（健康检查周期）
sleep 5

# 再次请求，应该返回 503 或服务不可用
curl http://localhost:8088/api/users/health

# 重新启动服务
docker-compose start user-service

# 等待服务注册到 Nacos
sleep 10

# 再次请求，恢复正常
curl http://localhost:8088/api/users/health
```

---

## 8. 常见问题

### 8.1 服务注册失败

**问题现象**：
```
com.alibaba.nacos.api.exception.NacosException: 
failed to req API:/nacos/v1/ns/instance after all servers
```

**解决方案**：
```bash
# 1. 检查 Nacos 是否启动
curl http://localhost:8848/nacos

# 2. 检查网络连通性
telnet localhost 8848

# 3. 检查配置是否正确
# 确认 spring.cloud.nacos.discovery.server-addr 配置

# 4. 查看 Nacos 日志
docker logs lite-gateway-nacos
```

### 8.2 服务发现不到

**问题现象**：
网关返回 503 或无法找到服务实例

**排查步骤**：
```bash
# 1. 确认服务已注册
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service"

# 2. 检查服务是否健康
# 访问 Nacos 控制台查看健康实例数

# 3. 检查网关配置
curl http://localhost:8080/api/config/gateway

# 4. 手动刷新网关配置
curl -X POST http://localhost:8080/api/config/refresh
```

### 8.3 配置不生效

**问题现象**：
修改 Nacos 配置后，服务没有热更新

**解决方案**：
```java
// 1. 确认添加了 @RefreshScope 注解
@RestController
@RefreshScope
public class ConfigController {
    // ...
}

// 2. 检查配置 Data ID 是否正确
// 格式: ${spring.application.name}-${spring.profiles.active}.${file-extension}
// 例如: user-service-dev.yaml

// 3. 确认命名空间和分组
spring.cloud.nacos.config.namespace=
spring.cloud.nacos.config.group=DEFAULT_GROUP
```

### 8.4 负载均衡不生效

**问题现象**：
多个实例时，请求总是打到同一个实例

**解决方案**：
```yaml
# 1. 确认负载均衡配置
spring:
  cloud:
    loadbalancer:
      nacos:
        enabled: true

# 2. 检查实例权重
# 在 Nacos 控制台调整实例权重

# 3. 使用 Ribbon（旧版本）或 Spring Cloud LoadBalancer
```

### 8.5 性能优化建议

```yaml
# Nacos 客户端优化
spring:
  cloud:
    nacos:
      discovery:
        # 心跳间隔，默认 5s
        heart-beat-interval: 5000
        # 心跳超时，默认 15s
        heart-beat-timeout: 15000
        # 删除超时实例，默认 30s
        ip-delete-timeout: 30000
        # 拉取服务列表间隔，默认 30s
        watch-delay: 30000
      config:
        # 配置长轮询超时，默认 30s
        timeout: 30000
        # 配置重试次数
        max-retry: 3
```

---

## 附录

### A. 常用 API 列表

```bash
# 服务注册
POST /nacos/v1/ns/instance

# 服务发现
GET /nacos/v1/ns/instance/list?serviceName={serviceName}

# 服务订阅
POST /nacos/v1/ns/instance/list

# 配置发布
POST /nacos/v1/cs/configs

# 配置获取
GET /nacos/v1/cs/configs?dataId={dataId}&group={group}

# 配置监听
POST /nacos/v1/cs/configs/listener
```

### B. 环境变量参考

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| NACOS_SERVER | Nacos 服务器地址 | localhost:8848 |
| NACOS_NAMESPACE | 命名空间 | (空) |
| NACOS_GROUP | 分组 | DEFAULT_GROUP |
| NACOS_ENABLED | 是否启用 Nacos | true |

### C. 版本兼容性

| 组件 | 版本 | 说明 |
|------|------|------|
| Nacos Server | 2.2.3 | 服务端 |
| Spring Cloud Alibaba | 2022.0.0.0 | 客户端 |
| Spring Boot | 3.2.0 | 基础框架 |
| Spring Cloud | 2023.0.0 | 微服务框架 |

---

**文档版本**: 1.0  
**更新日期**: 2024-03-05  
**适用项目**: Lite Gateway Suite
