#!/bin/bash

# 启动所有中间件（Redis + Nacos）
# 用途：一键启动所有必需的基础设施

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║          Lite Gateway 中间件一键启动脚本                       ║"
echo "║          Start All Middleware (Redis + Nacos)                 ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ 错误：未安装 Docker${NC}"
    exit 1
fi

# 创建网络
echo -e "${YELLOW}[1/3] 创建 Docker 网络...${NC}"
if ! docker network ls | grep -q "lite-gateway-network"; then
    docker network create lite-gateway-network
    echo -e "${GREEN}✅ 网络创建成功${NC}"
else
    echo -e "${GREEN}✅ 网络已存在${NC}"
fi

# 启动 Redis
echo -e "${YELLOW}[2/3] 启动 Redis...${NC}"
docker-compose -f docker-compose.redis.yml up -d
echo -e "${GREEN}✅ Redis 启动成功${NC}"

# 启动 Nacos
echo -e "${YELLOW}[3/3] 启动 Nacos...${NC}"
docker-compose -f docker-compose.nacos.yml up -d
echo -e "${GREEN}✅ Nacos 启动成功${NC}"

# 等待服务就绪
echo ""
echo -e "${BLUE}⏳ 等待服务就绪...${NC}"
sleep 10

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  🎉 所有中间件启动成功！${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}📍 Redis 连接信息：${NC}"
echo -e "   🌐 主机: ${YELLOW}localhost${NC}"
echo -e "   🔌 端口: ${YELLOW}6379${NC}"
echo -e "   🔗 连接: ${YELLOW}redis://localhost:6379${NC}"
echo ""
echo -e "${BLUE}📍 Nacos 连接信息：${NC}"
echo -e "   🌐 控制台: ${YELLOW}http://localhost:8848/nacos${NC}"
echo -e "   👤 账号: ${YELLOW}nacos${NC}"
echo -e "   🔑 密码: ${YELLOW}nacos${NC}"
echo ""
echo -e "${BLUE}📝 常用命令：${NC}"
echo -e "   查看所有容器: ${YELLOW}docker ps${NC}"
echo -e "   停止所有: ${YELLOW}docker-compose -f docker-compose.redis.yml down && docker-compose -f docker-compose.nacos.yml down${NC}"
echo -e "   查看 Redis 日志: ${YELLOW}docker logs -f lite-gateway-redis${NC}"
echo -e "   查看 Nacos 日志: ${YELLOW}docker logs -f lite-gateway-nacos${NC}"
echo ""
echo -e "${GREEN}现在可以在 IDE 中启动你的应用服务了！${NC}"
