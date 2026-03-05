@echo off
chcp 65001 >nul

:: 启动所有中间件（Redis + Nacos）(Windows)
:: 用途：一键启动所有必需的基础设施

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                                                               ║
echo ║          Lite Gateway 中间件一键启动脚本                       ║
echo ║          Start All Middleware (Redis + Nacos)                 ║
echo ║                                                               ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

:: 检查 Docker
where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] 未安装 Docker
    pause
    exit /b 1
)

:: 创建网络
echo [1/3] 创建 Docker 网络...
docker network ls | findstr "lite-gateway-network" >nul
if %errorlevel% neq 0 (
    docker network create lite-gateway-network
    echo [OK] 网络创建成功
) else (
    echo [OK] 网络已存在
)

:: 启动 Redis
echo [2/3] 启动 Redis...
docker-compose -f docker-compose.redis.yml up -d
echo [OK] Redis 启动成功

:: 启动 Nacos
echo [3/3] 启动 Nacos...
docker-compose -f docker-compose.nacos.yml up -d
echo [OK] Nacos 启动成功

:: 等待服务就绪
echo.
echo [INFO] 等待服务就绪...
timeout /t 10 /nobreak >nul

echo.
echo ═══════════════════════════════════════════════════════════════
echo   [SUCCESS] 所有中间件启动成功！
echo ═══════════════════════════════════════════════════════════════
echo.
echo Redis 连接信息：
echo   [主机] localhost
echo   [端口] 6379
echo   [连接] redis://localhost:6379
echo.
echo Nacos 连接信息：
echo   [控制台] http://localhost:8848/nacos
echo   [账号] nacos
echo   [密码] nacos
echo.
echo 常用命令：
echo   查看所有容器: docker ps
echo   停止所有: docker-compose -f docker-compose.redis.yml down ^&^& docker-compose -f docker-compose.nacos.yml down
echo   查看 Redis 日志: docker logs -f lite-gateway-redis
echo   查看 Nacos 日志: docker logs -f lite-gateway-nacos
echo.
echo 现在可以在 IDE 中启动你的应用服务了！

pause
