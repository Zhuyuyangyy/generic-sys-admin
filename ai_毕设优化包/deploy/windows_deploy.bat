@echo off
chcp 65001 >nul
echo ============================================================
echo   通用后台管理系统 - Windows一键部署脚本
echo ============================================================
echo.

:: 设置颜色
set GREEN=[92m
set RED=[91m
set YELLOW=[93m
set RESET=[0m

:: 获取脚本所在目录
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set BACKEND_DIR=%PROJECT_DIR%\backend
set FRONTEND_DIR=%PROJECT_DIR%\frontend
set SQL_DIR=%PROJECT_DIR%\sql

:: 切换到项目目录
cd /d "%PROJECT_DIR%"

echo %GREEN%[1/6]%RESET% 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo %RED%[错误]%RESET% 未检测到Java，请先安装JDK 17+
    echo   下载地址：https://adoptium.net/
    pause
    exit /b 1
)
java -version 2>&1 | findstr "version"
echo %GREEN%[OK]%RESET% Java环境正常

echo.
echo %GREEN%[2/6]%RESET% 检查Node.js环境...
node -v >nul 2>&1
if errorlevel 1 (
    echo %RED%[错误]%RESET% 未检测到Node.js，请先安装Node.js 18+
    echo   下载地址：https://nodejs.org/
    pause
    exit /b 1
)
node -v
echo %GREEN%[OK]%RESET% Node.js环境正常

echo.
echo %GREEN%[3/6]%RESET% 检查MySQL服务...
net start | findstr "MySQL" >nul 2>&1
if errorlevel 1 (
    echo %YELLOW%[警告]%RESET% MySQL服务未启动，尝试启动...
    net start MySQL80 >nul 2>&1
    if errorlevel 1 (
        net start MySQL >nul 2>&1
        if errorlevel 1 (
            echo %RED%[错误]%RESET% 无法启动MySQL服务，请手动启动
            pause
            exit /b 1
        )
    )
)
echo %GREEN%[OK]%RESET% MySQL服务正常

echo.
echo %GREEN%[4/6]%RESET% 初始化数据库...
echo   请确保MySQL的root密码为123456，或修改backend配置文件中的密码
set /p CONFIRM="   是否执行数据库初始化？(Y/N): "
if /i "%CONFIRM%"=="Y" (
    mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS generic_sys_admin CHARACTER SET utf8mb4;" 2>nul
    if errorlevel 1 (
        echo %YELLOW%[警告]%RESET% 数据库创建失败，可能密码不对
        echo   请手动执行：mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS generic_sys_admin"
    ) else (
        mysql -u root -p123456 generic_sys_admin < "%SQL_DIR%\v1.0__init.sql" 2>nul
        if errorlevel 1 (
            echo %YELLOW%[警告]%RESET% SQL脚本执行失败
            echo   请手动执行：mysql -u root -p generic_sys_admin ^< sql\v1.0__init.sql
        ) else (
            echo %GREEN%[OK]%RESET% 数据库初始化完成
        )
    )
) else (
    echo   跳过数据库初始化
)

echo.
echo %GREEN%[5/6]%RESET% 检查后端依赖并启动...
echo   首次运行会下载Maven依赖，请耐心等待...
cd /d "%BACKEND_DIR%"
start "SpringBoot后端" cmd /k "mvn spring-boot:run"

echo.
echo %GREEN%[6/6]%RESET% 安装前端依赖并启动...
cd /d "%FRONTEND_DIR%"
start "Vue3前端" cmd /k "npm install && npm run dev"

echo.
echo ============================================================
echo   %GREEN%部署完成！%RESET%
echo ============================================================
echo.
echo   前端地址：http://localhost:5173
echo   后端地址：http://localhost:8080
echo   API文档：http://localhost:8080/doc.html
echo.
echo   默认账号：admin / 123456
echo   注意：后端和前端已在新窗口启动，请勿关闭！
echo.
pause
