# 测试服务说明

## 服务列表

### 1. user-service (用户服务)
- **端口**: 9001
- **基础路径**: `/api/users`
- **功能**: 用户CRUD操作

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/health` | 健康检查 |
| GET | `/api/users` | 获取用户列表 |
| GET | `/api/users/{id}` | 获取用户详情 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |
| GET | `/api/users/profile` | 获取当前用户信息 |

#### 测试命令
```bash
# 健康检查
curl http://localhost:9001/api/users/health

# 获取用户列表
curl http://localhost:9001/api/users

# 获取用户详情
curl http://localhost:9001/api/users/1

# 创建用户
curl -X POST http://localhost:9001/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"赵六","email":"zhaoliu@example.com","phone":"13800138006"}'

# 更新用户
curl -X PUT http://localhost:9001/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"张三 Updated","email":"zhangsan@example.com","phone":"13800138001"}'

# 删除用户
curl -X DELETE http://localhost:9001/api/users/4
```

### 2. order-service (订单服务)
- **端口**: 9002
- **基础路径**: `/api/orders`
- **功能**: 订单管理

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/orders/health` | 健康检查 |
| GET | `/api/orders` | 获取订单列表 |
| GET | `/api/orders/{id}` | 获取订单详情 |
| POST | `/api/orders` | 创建订单 |
| PUT | `/api/orders/{id}/status` | 更新订单状态 |
| GET | `/api/orders/user/{userId}` | 获取用户订单 |
| GET | `/api/orders/stats` | 获取订单统计 |

#### 测试命令
```bash
# 健康检查
curl http://localhost:9002/api/orders/health

# 获取订单列表
curl http://localhost:9002/api/orders

# 获取订单详情
curl http://localhost:9002/api/orders/1

# 创建订单
curl -X POST http://localhost:9002/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"amount":399.99}'

# 更新订单状态
curl -X PUT "http://localhost:9002/api/orders/1/status?status=PAID"

# 获取用户订单
curl http://localhost:9002/api/orders/user/1

# 获取订单统计
curl http://localhost:9002/api/orders/stats
```

## 启动方式

### 本地启动
```bash
# 启动 user-service
cd user-service
mvn spring-boot:run

# 启动 order-service
cd order-service
mvn spring-boot:run
```

### Docker 启动
```bash
# 构建镜像
cd user-service
mvn clean package
docker build -t user-service .

cd order-service
mvn clean package
docker build -t order-service .

# 运行容器
docker run -d -p 9001:9001 --name user-service user-service
docker run -d -p 9002:9002 --name order-service order-service
```

## 网关路由配置

在 lite-gateway-admin 管理后台配置以下路由：

### Static 类型路由

1. **用户服务路由**
   - 路由ID: user-service-route
   - 路径: `/api/users/**`
   - 目标URL: `http://localhost:9001`

2. **订单服务路由**
   - 路由ID: order-service-route
   - 路径: `/api/orders/**`
   - 目标URL: `http://localhost:9002`

### Nacos 类型路由（可选）

如果启用了 Nacos，可以配置为服务发现模式：

1. **用户服务路由**
   - 路由ID: user-service-nacos
   - 路径: `/api/users/**`
   - 服务名: `user-service`

2. **订单服务路由**
   - 路由ID: order-service-nacos
   - 路径: `/api/orders/**`
   - 服务名: `order-service`

## 通过网关访问

配置完成后，可以通过网关访问测试服务：

```bash
# 网关端口默认 8088

# 访问用户服务（通过网关）
curl http://localhost:8088/api/users/health
curl http://localhost:8088/api/users

# 访问订单服务（通过网关）
curl http://localhost:8088/api/orders/health
curl http://localhost:8088/api/orders
```
