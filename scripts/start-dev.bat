@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

echo ============================================
echo   面经阁 · 一键启动开发环境
echo ============================================
echo.

if not exist .env (
    copy .env.example .env >nul
    echo [!] 未找到 .env，已从 .env.example 复制一份
    echo [!] 请编辑项目根目录的 .env 填入真实数据库密码 / JWT 密钥后，重新运行本脚本
    pause
    exit /b 1
)

rem 把 .env 加载进当前进程（值内请勿包含 %% 字符）
for /f "usebackq tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"

echo [1/3] 启动 MySQL + Redis（需 Docker Desktop 已运行）...
docker compose up -d
if errorlevel 1 (
    echo [!] Docker 启动失败，请确认 Docker Desktop 正在运行
    pause
    exit /b 1
)

if not exist backend\target\backend-0.1.0-SNAPSHOT.jar (
    echo [!] 未找到后端 jar，请先在 IDEA 里执行一次 Maven package 再运行本脚本
    pause
    exit /b 1
)

echo [2/3] 启动后端 8080（独立窗口，关闭该窗口即停止后端）...
start "mjg-backend" /d "%CD%" cmd /k "java -jar backend\target\backend-0.1.0-SNAPSHOT.jar"

echo [3/3] 启动前端 5173（独立窗口）...
start "mjg-frontend" /d "%CD%\frontend" cmd /k "npm run dev"

echo.
echo 全部启动完成：
echo   前端页面   http://localhost:5173
echo   接口文档   http://localhost:8080/doc.html
echo   停止环境   运行 scripts\stop-dev.bat
echo.
pause
