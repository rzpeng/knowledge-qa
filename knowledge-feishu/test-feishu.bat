@echo off
echo 正在检查飞书模块配置...

echo 检查1: 检查项目结构
if exist "src\main\java\com\knowledge\feishu\" (
    echo [✓] 项目结构正确
) else (
    echo [✗] 项目结构错误
    pause
    exit /b 1
)

echo 检查2: 检查核心文件
if exist "src\main\java\com\knowledge\feishu\FeishuApplication.java" (
    echo [✓] 主应用程序文件存在
) else (
    echo [✗] 主应用程序文件不存在
    pause
    exit /b 1
)

echo 检查3: 检查配置文件
if exist "src\main\resources\application.yml" (
    echo [✓] 配置文件存在
) else (
    echo [✗] 配置文件不存在
    pause
    exit /b 1
)

echo 检查4: 检查构建文件
if exist "build.gradle" (
    echo [✓] 构建文件存在
) else (
    echo [✗] 构建文件不存在
    pause
    exit /b 1
)

echo 检查5: 检查依赖配置
findstr /C:"spring-boot-starter-web" build.gradle >nul
if %errorlevel% equ 0 (
    echo [✓] Web依赖配置正确
) else (
    echo [✗] Web依赖配置错误
    pause
    exit /b 1
)

echo.
echo ========================================
echo 飞书模块配置检查完成！
echo ========================================
echo.
echo 启动说明:
echo 1. 确保已安装Java 17+和Gradle
echo 2. 配置飞书应用信息到application.yml
echo 3. 运行: gradle bootRun
echo 4. 访问: http://localhost:8081/api/feishu/health
echo.
pause