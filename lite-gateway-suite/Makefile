# Lite Gateway Suite - Makefile
.PHONY: up down dev build clean logs

# 一键启动所有服务
up:
	docker-compose up -d

# 停止所有服务
down:
	docker-compose down

# 查看日志
logs:
	docker-compose logs -f

# 构建所有镜像
build:
	docker-compose build

# 清理所有容器和卷
clean:
	docker-compose down -v
	docker system prune -f

# 开发模式 - 只启动基础设施（Redis + Nacos）
dev-infra:
	docker-compose up -d redis nacos

# 开发模式 - 本地启动Admin
run-admin:
	cd lite-gateway-admin && mvn spring-boot:run

# 开发模式 - 本地启动Gateway
run-gateway:
	cd lite-gateway-core && mvn spring-boot:run

# 开发模式 - 本地启动UI
dev-ui:
	cd lite-gateway-ui && npm install && npm run dev

# 打包所有服务
package:
	cd lite-gateway-admin && mvn clean package -DskipTests
	cd lite-gateway-core && mvn clean package -DskipTests
	cd lite-gateway-ui && npm install && npm run build

# 帮助
help:
	@echo "Lite Gateway Suite - 可用命令:"
	@echo "  make up          - 一键启动所有服务"
	@echo "  make down        - 停止所有服务"
	@echo "  make dev-infra   - 只启动基础设施(Redis+Nacos)"
	@echo "  make run-admin   - 本地启动Admin服务"
	@echo "  make run-gateway - 本地启动Gateway服务"
	@echo "  make dev-ui      - 本地启动UI开发服务器"
	@echo "  make build       - 构建所有Docker镜像"
	@echo "  make package     - 打包所有服务"
	@echo "  make logs        - 查看日志"
	@echo "  make clean       - 清理所有容器和卷"
