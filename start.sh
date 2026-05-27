#!/bin/bash
# generic-sys-admin 启动脚本
# 用法: ./start.sh [command]
#   无参数  - 启动系统监控面板 (http://localhost:5000)
#   list    - 显示所有工具
#   all     - 运行全部检查工具
#   check-aiclient / analyze-fc / fix-all 等 - 运行指定工具
#   dashboard - 启动Web面板

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$SCRIPT_DIR"

# 检查Python
if ! command -v python3 &>/dev/null; then
    echo "[错误] 未找到 python3，请安装 Python 3.8+"
    exit 1
fi

# 管理虚拟环境
VENV_DIR="$SCRIPT_DIR/.venv"
if [ ! -d "$VENV_DIR" ]; then
    echo "[generic-sys-admin] 创建虚拟环境..."
    python3 -m venv "$VENV_DIR"
fi
source "$VENV_DIR/bin/activate"

# 安装依赖
echo "[generic-sys-admin] 安装依赖..."
pip install -q -r requirements.txt

# 启动
if [ -z "$1" ]; then
    echo "[generic-sys-admin] 启动系统监控面板..."
    echo "[generic-sys-admin] 访问 http://localhost:5000"
    python3 main.py
else
    python3 main.py "$@"
fi