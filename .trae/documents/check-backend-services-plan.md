# 后端服务启动检查计划

## 目标
检查 `lite-gateway-admin` 和 `lite-gateway-core` 两个后端服务是否能正常启动和运行。

## 服务依赖分析

### 1. lite-gateway-admin (端口: 8080)
**技术栈**: Spring Boot 3.2.0 + Java 17
**外部依赖**:
- Redis (配置存储)
- Nacos (可选，配置中心)

**配置检查项**:
- [ ] application.yml 配置正确性
- [ ] Redis 连接配置
- [ ] Nacos 配置中心连接
- [ ] JWT 密钥配置

### 2. lite-gateway-core (端口: 8088)
**技术栈**: Spring Cloud Gateway + Spring Boot 3.2.0 + Java 17
**外部依赖**:
- Redis (配置存储 + 订阅通知)
- Nacos (服务发现 + 配置中心)

**配置检查项**:
- [ ] application.yml 配置正确性
- [ ] bootstrap.yml 配置正确性
- [ ] Redis 连接配置
- [ ] Nacos 服务发现和配置中心连接
- [ ] Gateway 路由配置

## 执行步骤

### 阶段一: 环境检查
1. **检查 Java 环境**
   - 确认 Java 17 已安装
   - 确认 JAVA_HOME 设置正确

2. **检查 Maven 环境**
   - 确认 Maven 已安装
   - 确认 mvn 命令可用

3. **检查基础设施**
   - Redis 是否可连接 (默认: localhost:6379)
   - Nacos 是否可连接 (默认: localhost:8848)

### 阶段二: 编译检查
1. **编译 lite-gateway-admin**
   ```bash
   cd lite-gateway-suite/lite-gateway-admin
   mvn clean compile
   ```

2. **编译 lite-gateway-core**
   ```bash
   cd lite-gateway-suite/lite-gateway-core
   mvn clean compile
   ```

### 阶段三: 启动测试
1. **启动 lite-gateway-admin**
   ```bash
   cd lite-gateway-suite/lite-gateway-admin
   mvn spring-boot:run
   ```
   - 检查启动日志是否有错误
   - 验证端口 8080 是否监听
   - 测试健康检查端点

2. **启动 lite-gateway-core**
   ```bash
   cd lite-gateway-suite/lite-gateway-core
   mvn spring-boot:run
   ```
   - 检查启动日志是否有错误
   - 验证端口 8088 是否监听
   - 测试健康检查端点

### 阶段四: 功能验证
1. **Admin 服务验证**
   - 访问 Swagger UI: http://localhost:8080/swagger-ui.html
   - 测试路由管理 API

2. **Gateway 服务验证**
   - 检查 Gateway 路由端点: http://localhost:8088/actuator/gateway/routes
   - 验证健康检查: http://localhost:8088/actuator/health

## 潜在问题及解决方案

### 问题 1: Redis 连接失败
**症状**: 启动时报 Redis 连接错误
**解决方案**:
- 启动本地 Redis: `docker run -d -p 6379:6379 redis:7-alpine`
- 或使用 Makefile: `make dev-infra`

### 问题 2: Nacos 连接失败
**症状**: 启动时报 Nacos 连接超时
**解决方案**:
- 启动本地 Nacos: `docker run -d -p 8848:8848 nacos/nacos-server:v2.2.3 -e MODE=standalone`
- 或修改配置禁用 Nacos: 在 bootstrap.yml 中设置 `enabled: false`

### 问题 3: 端口冲突
**症状**: 端口已被占用
**解决方案**:
- 修改 application.yml 中的 server.port
- 或关闭占用端口的进程

### 问题 4: Maven 依赖下载失败
**症状**: 编译时依赖下载失败
**解决方案**:
- 检查网络连接
- 配置 Maven 镜像源
- 使用 `mvn clean compile -U` 强制更新依赖

## 预期结果

### 成功标准
- [ ] 两个服务都能正常编译无错误
- [ ] 两个服务都能正常启动无异常
- [ ] Admin 服务 Swagger UI 可访问
- [ ] Gateway 服务 Actuator 端点可访问

### 失败处理
如果服务启动失败，需要:
1. 收集完整的错误日志
2. 分析错误原因
3. 提供修复建议

## 执行命令汇总

```bash
# 1. 检查环境
java -version
mvn -version

# 2. 启动基础设施（可选，如果没有本地 Redis/Nacos）
cd lite-gateway-suite
make dev-infra

# 3. 编译 Admin 服务
cd lite-gateway-suite/lite-gateway-admin
mvn clean compile

# 4. 编译 Core 服务
cd lite-gateway-suite/lite-gateway-core
mvn clean compile

# 5. 启动 Admin 服务（终端1）
cd lite-gateway-suite/lite-gateway-admin
mvn spring-boot:run

# 6. 启动 Core 服务（终端2）
cd lite-gateway-suite/lite-gateway-core
mvn spring-boot:run

# 7. 验证服务
curl http://localhost:8080/actuator/health
curl http://localhost:8088/actuator/health
```
