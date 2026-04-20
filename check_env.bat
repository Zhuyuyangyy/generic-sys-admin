@echo off
chcp 65001 >nul
:: ==========================================================
::  🏆 毕设工厂 · 环境检测脚本
::  功能：检测 Java / MySQL / 端口连通性
::  适用：接单前环境自查、客户现场快速诊断
:: ==========================================================

echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║   🏭 毕设工厂 · 环境检测工具   v1.0                   ║
echo ╚══════════════════════════════════════════════════════╝
echo.

:: 检测结果计数
set /a PASS=0
set /a FAIL=0

:: ========== 1. 检测 JAVA_HOME ==========
echo [1/4] 检测 Java 运行环境...
echo.

if "%JAVA_HOME%"=="" (
    echo     ❌ JAVA_HOME 未设置
    echo.
    echo     请按以下步骤修复：
    echo       1. 右键 [此电脑] -^> [属性] -^> [高级系统设置]
    echo       2. 点 [环境变量]
    echo       3. 系统变量 -^> 新建：
    echo          变量名：JAVA_HOME
    echo          变量值：C:\Program Files\Java\jdk17   ^(路径按实际安装目录^)
    echo       4. 编辑 Path，添加：%%JAVA_HOME%%\bin
    echo       5. 重启命令行后再次运行此脚本
    set /a FAIL+=1
) else (
    :: 检测 java 命令是否可用
    java -version 2>nul
    if errorlevel 1 (
        echo     ❌ JAVA_HOME 已设置但 Java 命令不可用
        echo       请检查 %%JAVA_HOME%%\bin 是否在 PATH 中
        set /a FAIL+=1
    ) else (
        echo     ✅ JAVA_HOME: %JAVA_HOME%
        for /f "tokens=*" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
            echo        %%v
        )
        set /a PASS+=1
    )
)

echo.

:: ========== 2. 检测 MySQL 端口 3306 ==========
echo [2/4] 检测 MySQL 端口 3306...
echo.

:: 使用 PowerShell 的 Test-NetConnection（更可靠）
powershell -Command "Test-NetConnection -ComputerName localhost -Port 3306 -InformationLevel Quiet" >nul 2>&1
if errorlevel 1 (
    :: fallback：用 telnet 风格的连接测试（Windows 原生）
    powershell -Command "
        $tcp = New-Object System.Net.Sockets.TcpClient
        try {
            $tcp.Connect('localhost', 3306)
            $tcp.Close()
            exit 0
        } catch {
            exit 1
        }
    " >nul 2>&1
    if errorlevel 1 (
        echo     ❌ MySQL 端口 3306 未开放
        echo.
        echo     请检查：
        echo       1. MySQL 服务是否已启动？
        echo          [Win+R] -^> services.msc -^> 找 MySQL -^> 启动
        echo       2. 端口是否被占用？
        echo          netstat -ano ^| findstr 3306
        echo       3. 防火墙是否拦截？
        echo          netsh firewall add portopening TCP 3306 MySQL
        set /a FAIL+=1
    ) else (
        echo     ✅ MySQL 端口 3306 连通
        set /a PASS+=1
    )
) else (
    echo     ✅ MySQL 端口 3306 连通
    set /a PASS+=1
)

echo.

:: ========== 3. 检测 Java 后端端口 8080 ==========
echo [3/4] 检测 Java 后端端口 8080...
echo.

powershell -Command "
    $tcp = New-Object System.Net.Sockets.TcpClient
    try {
        $tcp.Connect('localhost', 8080)
        $tcp.Close()
        exit 0
    } catch {
        exit 1
    }
" >nul 2>&1

if errorlevel 1 (
    echo     ⚠️  端口 8080 未开放（Java 后端可能未启动）
    echo.
    echo     如需启动后端：
    echo       cd D:\ZYY Project\generic-sys-admin\backend
    echo       mvn spring-boot:run
    echo.
    echo     启动后端后再运行此脚本确认
    set /a FAIL+=1
) else (
    echo     ✅ 端口 8080 连通（Java 后端已启动）
    set /a PASS+=1
)

echo.

:: ========== 4. 综合检测报告 ==========
echo [4/4] 生成检测报告...
echo.

echo ╔══════════════════════════════════════════════════════╗
echo ║                   📊 检测结果汇总                      ║
echo ╠══════════════════════════════════════════════════════╣
echo ║                                                              ║
if %PASS%==3 (
    echo ║     🎉 全部检测通过！环境就绪，可以开始开发！           ║
) else (
    echo ║     ⚠️  检测完成，存在 %FAIL% 项问题，请根据上文修复      ║
)
echo ║                                                              ║
echo ╠══════════════════════════════════════════════════════╣
echo ║  ✅ 通过：%PASS% 项                                             ║
if %FAIL% gtr 0 (
    echo ║  ❌ 失败：%FAIL% 项                                             ║
)
echo ╚══════════════════════════════════════════════════════╝
echo.

:: ========== 5. 快速启动建议 ==========
if %FAIL% gtr 0 (
    echo ──────────────────────────────────────────────────────
    echo   💡 快速启动指南：
    echo.
    echo   1. 启动 MySQL
    echo      net start mysql
    echo.
    echo   2. 初始化数据库
    echo      mysql -u root -p ^< sql\v1.0__init.sql
    echo.
    echo   3. 启动后端
    echo      cd backend
    echo      mvn spring-boot:run
    echo.
    echo   4. 启动前端
    echo      cd frontend
    echo      npm run dev
    echo.
    echo   5. 访问系统
    echo      前端：http://localhost:5173
    echo      API文档：http://localhost:8080/doc.html
    echo      测试账号：admin / 123456
    echo ──────────────────────────────────────────────────────
)

echo.
echo 按任意键退出...
pause >nul
