@echo off
chcp 65001 >nul

:: Redis 独立启动脚本 (Windows)
:: 用途：单独启动 Redis 缓存服务

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                                                               ║
echo ║              Redis 缓存服务启动脚本                            ║
echo ║              Redis Server Launcher                            ║
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
echo [1/2] 创建 Docker 网络...
docker network ls | findstr "lite-gateway-network" >nul
if %errorlevel% neq 0 (
    docker network create lite-gateway-network
    echo [OK] 网络创建成功
) else (
    echo [OK] 网络已存在
)

:: 启动 Redis
echo [2/2] 启动 Redis 服务...
docker-compose -f docker-compose.redis.yml up -d

:: 等待启动
timeout /t 3 /nobreak >nul

:: 检查状态
docker ps | findstr "lite-gateway-redis" >nul
if %errorlevel% equ 0 (
    echo.
    echo ═══════════════════════════════════════════════════════════════
    echo   [SUCCESS] Redis 启动成功！
    echo ═══════════════════════════════════════════════════════════════
    echo.
    echo 连接信息：
    echo   [主机] localhost
    echo   [端口] 6379
    echo   [密码] 无（开发环境）
    echo.
    echo 连接字符串：
    echo   redis://localhost:6379
    echo.
    echo 常用命令：
    echo   进入容器: docker exec -it lite-gateway-redis redis-cli
    echo   查看日志: docker logs -f lite-gateway-redis
    echo   停止服务: docker-compose -f docker-compose.redis.yml down
    echo.
) else (
    echo [ERROR] Redis 启动失败
    echo 请检查日志: docker logs lite-gateway-redis
    pause
    exit /b 1
)

pause
