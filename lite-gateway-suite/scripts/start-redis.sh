#!/bin/bash

# Redis 独立启动脚本
# 用途：单独启动 Redis 缓存服务

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║              Redis 缓存服务启动脚本                            ║"
echo "║              Redis Server Launcher                            ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ 错误：未安装 Docker${NC}"
    exit 1
fi

# 创建网络
echo -e "${YELLOW}[1/2] 创建 Docker 网络...${NC}"
if ! docker network ls | grep -q "lite-gateway-network"; then
    docker network create lite-gateway-network
    echo -e "${GREEN}✅ 网络创建成功${NC}"
else
    echo -e "${GREEN}✅ 网络已存在${NC}"
fi

# 启动 Redis
echo -e "${YELLOW}[2/2] 启动 Redis 服务...${NC}"
docker-compose -f docker-compose.redis.yml up -d

# 等待启动
sleep 3

# 检查状态
if docker ps | grep -q "lite-gateway-redis"; then
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ✅ Redis 启动成功！${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${BLUE}📍 连接信息：${NC}"
    echo -e "   🌐 主机: ${YELLOW}localhost${NC}"
    echo -e "   🔌 端口: ${YELLOW}6379${NC}"
    echo -e "   🔑 密码: ${YELLOW}无（开发环境）${NC}"
    echo ""
    echo -e "${BLUE}🔗 连接字符串：${NC}"
    echo -e "   ${YELLOW}redis://localhost:6379${NC}"
    echo ""
    echo -e "${BLUE}📝 常用命令：${NC}"
    echo -e "   进入容器: ${YELLOW}docker exec -it lite-gateway-redis redis-cli${NC}"
    echo -e "   查看日志: ${YELLOW}docker logs -f lite-gateway-redis${NC}"
    echo -e "   停止服务: ${YELLOW}docker-compose -f docker-compose.redis.yml down${NC}"
    echo ""
else
    echo -e "${RED}❌ Redis 启动失败${NC}"
    echo "请检查日志: docker logs lite-gateway-redis"
    exit 1
fi
