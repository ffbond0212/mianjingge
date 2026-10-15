@echo off
chcp 65001 >nul
cd /d "%~dp0.."

echo ============================================
echo   面经阁 · 停止开发环境
echo ============================================
echo.

echo [x] 停止后端窗口（未在运行则无操作）...
taskkill /FI "WINDOWTITLE eq mjg-backend*" /T /F >nul 2>&1

echo [x] 停止前端窗口（未在运行则无操作）...
taskkill /FI "WINDOWTITLE eq mjg-frontend*" /T /F >nul 2>&1

echo [x] 停止 MySQL + Redis 容器（数据保留在 Docker 卷中）...
docker compose stop

echo.
echo 完成。面试数据都在 Docker 卷里，下次运行 scripts\start-dev.bat 即可继续。
echo 提示：从 IDEA / VS Code 启动的前后端窗口请在原工具中停止。
echo.
pause
