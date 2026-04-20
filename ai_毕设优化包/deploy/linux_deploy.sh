#!/bin/bash

# ============================================================
#   通用后台管理系统 - Linux一键部署脚本
#   使用方法：bash deploy/linux_deploy.sh
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"
SQL_DIR="$PROJECT_DIR/sql"

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}  通用后台管理系统 - Linux一键部署脚本${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# ----------------------------------------------------------
# 1. 检查Java环境
# ----------------------------------------------------------
echo -e "${GREEN}[1/7]${NC} 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}[错误]${NC} 未检测到Java，请先安装JDK 17+"
    echo "  CentOS/RHEL: sudo yum install -y java-17-openjdk java-17-openjdk-devel"
    echo "  Ubuntu/Debian: sudo apt install -y openjdk-17-jdk"
    exit 1
fi
java -version 2>&1 | head -1
echo -e "${GREEN}[OK]${NC} Java环境正常"

# ----------------------------------------------------------
# 2. 检查Node.js环境
# ----------------------------------------------------------
echo ""
echo -e "${GREEN}[2/7]${NC} 检查Node.js环境..."
if ! command -v node &> /dev/null; then
    echo -e "${RED}[错误]${NC} 未检测到Node.js，请先安装Node.js 18+"
    echo "  curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -"
    echo "  sudo apt-get install -y nodejs"
    exit 1
fi
node -v
echo -e "${GREEN}[OK]${NC} Node.js环境正常"

# ----------------------------------------------------------
# 3. 检查MySQL
# ----------------------------------------------------------
echo ""
echo -e "${GREEN}[3/7]${NC} 检查MySQL服务..."
if command -v mysql &> /dev/null; then
    if systemctl is-active --quiet mysql; then
        echo -e "${GREEN}[OK]${NC} MySQL服务正常运行"
    elif systemctl is-active --quiet mysqld; then
        echo -e "${GREEN}[OK]${NC} MySQL服务正常运行"
    else
        echo -e "${YELLOW}[警告]${NC} MySQL未运行，尝试启动..."
        sudo systemctl start mysql 2>/dev/null || sudo systemctl start mysqld 2>/dev/null || true
    fi
else
    echo -e "${YELLOW}[警告]${NC} 未检测到MySQL客户端，请确保MySQL服务已启动"
fi

# ----------------------------------------------------------
# 4. 初始化数据库
# ----------------------------------------------------------
echo ""
echo -e "${GREEN}[4/7]${NC} 初始化数据库..."
read -p "  请输入MySQL root密码 [默认无需密码直接回车]: " -s MYSQL_PASSWORD
echo ""

if [ -z "$MYSQL_PASSWORD" ]; then
    MYSQL_CMD="mysql -u root"
else
    MYSQL_CMD="mysql -u root -p$MYSQL_PASSWORD"
fi

# 创建数据库
$MYSQL_CMD -e "CREATE DATABASE IF NOT EXISTS generic_sys_admin CHARACTER SET utf8mb4;" 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}[OK]${NC} 数据库创建成功"
else
    echo -e "${YELLOW}[警告]${NC} 数据库创建失败，请手动检查MySQL连接"
fi

# 执行SQL脚本
if [ -f "$SQL_DIR/v1.0__init.sql" ]; then
    echo "  执行建表脚本..."
    $MYSQL_CMD generic_sys_admin < "$SQL_DIR/v1.0__init.sql" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[OK]${NC} 建表脚本执行成功"
    else
        echo -e "${YELLOW}[警告]${NC} 建表脚本执行失败，请手动执行"
    fi
else
    echo -e "${YELLOW}[警告]${NC} 未找到建表脚本: $SQL_DIR/v1.0__init.sql"
fi

# ----------------------------------------------------------
# 5. 启动后端
# ----------------------------------------------------------
echo ""
echo -e "${GREEN}[5/7]${NC} 启动后端服务..."
cd "$BACKEND_DIR"

# 创建日志目录
mkdir -p "$PROJECT_DIR/logs"

# 后台启动Spring Boot
nohup mvn spring-boot:run > "$PROJECT_DIR/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > "$PROJECT_DIR/logs/backend.pid"

echo -e "${GREEN}[OK]${NC} 后端已启动 (PID: $BACKEND_PID)"
echo "  日志文件: $PROJECT_DIR/logs/backend.log"
echo "  等待Spring Boot启动（约30秒）..."

# ----------------------------------------------------------
# 6. 安装前端依赖
# ----------------------------------------------------------
echo ""
echo -e "${GREEN}[6/7]${NC} 安装前端依赖（使用淘宝镜像）..."
cd "$FRONTEND_DIR"

# 设置npm镜像
npm config set registry https://registry.npmmirror.com

# 安装依赖
npm install --registry=https://registry.npmmirror.com

if [ $? -eq 0 ]; then
    echo -e "${GREEN}[OK]${NC} 前端依赖安装成功"
else
    echo -e "${YELLOW}[警告]${NC} 前端依赖安装失败，请检查网络"
fi

# ----------------------------------------------------------
# 7. 启动前端
# ----------------------------------------------------------
echo ""
echo -e "${GREEN}[7/7]${NC} 启动前端服务..."
nohup npm run dev > "$PROJECT_DIR/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > "$PROJECT_DIR/logs/frontend.pid"

echo -e "${GREEN}[OK]${NC} 前端已启动 (PID: $FRONTEND_PID)"
echo "  日志文件: $PROJECT_DIR/logs/frontend.log"

# ----------------------------------------------------------
# 完成
# ----------------------------------------------------------
echo ""
echo -e "${BLUE}============================================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""
echo -e "  前端地址: ${GREEN}http://localhost:5173${NC}"
echo -e "  后端地址: ${GREEN}http://localhost:8080${NC}"
echo -e "  API文档:  ${GREEN}http://localhost:8080/doc.html${NC}"
echo ""
echo -e "  默认账号: ${YELLOW}admin / 123456${NC}"
echo ""
echo -e "  停止服务命令:"
echo -e "    kill \$(cat $PROJECT_DIR/logs/backend.pid)"
echo -e "    kill \$(cat $PROJECT_DIR/logs/frontend.pid)"
echo ""
echo -e "  查看日志命令:"
echo -e "    tail -f $PROJECT_DIR/logs/backend.log"
echo -e "    tail -f $PROJECT_DIR/logs/frontend.log"
echo ""

# 等待后端启动
echo -e "${YELLOW}正在等待后端启动，请稍候...${NC}"
sleep 5

# 检查后端是否启动成功
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo -e "${GREEN}[OK]${NC} 后端启动成功！"
else
    echo -e "${YELLOW}[警告]${NC} 后端可能还在启动中，请等待约20秒后访问"
fi

echo ""
echo -e "${BLUE}请在浏览器中打开 http://localhost:5173 开始使用！${NC}"
