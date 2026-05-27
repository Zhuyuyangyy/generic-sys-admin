#!/usr/bin/env python3
"""
generic-sys-admin - AI开发者代码质量工具箱
整合29个代码检查/修复/编码处理/系统管理脚本
用法: python main.py <command>
"""
import sys
import os
import argparse

# 添加工具目录到路径
TOOLS = {
    # 代码质量检查
    'check-aiclient': 'check_aiclient.py',
    'analyze-fc': 'analyze_fc.py',
    'analyze-fc2': 'analyze_fc2.py',
    'check-encoding': 'check_encoding.py',
    'check-java': 'check_java.py',
    'check-line285': 'check_line285.py',

    # 编码修复
    'convert-encoding': 'convert_encoding.py',
    'fix-bom': 'fix_bom.py',
    'fix-bom-single': 'fix_bom_single.py',
    'fix-corrupted': 'fix_corrupted.py',
    'fix-fc': 'fix_fc.py',
    'fix-fc2': 'fix_fc2.py',
    'fix-fc3': 'fix_fc3.py',
    'fix-ai-exact': 'fix_ai_exact.py',
    'fix-ai-lines': 'fix_ai_lines.py',
    'fix-ai-python': 'fix_ai_python.py',
    'fix-all': 'fix_all.py',
    'rebuild-ai': 'rebuild_ai.py',
    'recover-fc': 'recover_fc.py',

    # Git/版本管理
    'analyze-git': 'analyze_git_ai.py',
    'git-to-gbk': 'git_to_gbk.py',

    # 日志/数据
    'create-log-table': 'create_log_table.py',
    'ai-line-check': 'ai_line_check.py',

    # 调试工具
    'debug2': 'debug2.py',
    'debug3': 'debug3.py',
    'read-file': 'read_file.py',
    'view-fc': 'view_fc.py',
    'test-login': 'test_login.py',

    # Web界面
    'dashboard': 'dashboard.py',
}


def run_tool(name):
    """执行指定的工具脚本"""
    script = TOOLS.get(name)
    if not script:
        print(f"[错误] 未知工具: {name}")
        print(f"       使用 python main.py list 查看所有工具")
        sys.exit(1)

    script_path = os.path.join(os.path.dirname(__file__), script)
    if not os.path.exists(script_path):
        print(f"[错误] 脚本不存在: {script}")
        sys.exit(1)

    print(f"[generic-sys-admin] 执行: {name} ({script})")
    os.execv(sys.executable, [sys.executable, script_path])


def list_tools():
    """列出所有可用工具"""
    print("=" * 55)
    print("generic-sys-admin - 开发者工具箱")
    print("=" * 55)
    print()
    print("  [代码质量检查]")
    for t in ['check-aiclient', 'analyze-fc', 'analyze-fc2',
              'check-encoding', 'check-java', 'check-line285']:
        print(f"    {t:<22} -> {TOOLS[t]}")

    print()
    print("  [编码修复]")
    for t in ['convert-encoding', 'fix-bom', 'fix-bom-single',
              'fix-corrupted', 'fix-fc', 'fix-fc2', 'fix-fc3',
              'fix-ai-exact', 'fix-ai-lines', 'fix-ai-python',
              'fix-all', 'rebuild-ai', 'recover-fc']:
        print(f"    {t:<22} -> {TOOLS[t]}")

    print()
    print("  [Git/版本管理]")
    for t in ['analyze-git', 'git-to-gbk']:
        print(f"    {t:<22} -> {TOOLS[t]}")

    print()
    print("  [日志/数据]")
    for t in ['create-log-table', 'ai-line-check']:
        print(f"    {t:<22} -> {TOOLS[t]}")

    print()
    print("  [调试工具]")
    for t in ['debug2', 'debug3', 'read-file', 'view-fc', 'test-login']:
        print(f"    {t:<22} -> {TOOLS[t]}")

    print()
    print("  [Web界面]")
    print(f"    {'dashboard':<22} -> {TOOLS['dashboard']}  (端口5000)")
    print()
    print("  [快速命令]")
    print("    all     - 运行全部检查")
    print("    list    - 显示本列表")
    print()
    print("用法示例:")
    print("  python main.py list")
    print("  python main.py check-aiclient")
    print("  python main.py analyze-fc")
    print("  python main.py fix-all")
    print("  python main.py dashboard")
    print()


def main():
    if len(sys.argv) < 2:
        # 无参数：启动Web监控面板
        print("[generic-sys-admin] 启动系统监控面板...")
        print("[generic-sys-admin] 访问 http://localhost:5000")
        print("[提示] 使用 python main.py list 查看所有工具")
        os.execv(sys.executable, [sys.executable,
            os.path.join(os.path.dirname(__file__), 'dashboard.py')])

    cmd = sys.argv[1]

    if cmd == 'list':
        list_tools()
    elif cmd == 'all':
        print("[generic-sys-admin] 执行全部工具...")
        results = []
        for name, script in TOOLS.items():
            if name == 'dashboard':
                continue
            sp = os.path.join(os.path.dirname(__file__), script)
            if os.path.exists(sp):
                print(f"\n>>> {name} ({script})")
                try:
                    r = os.system(f'{sys.executable} "{sp}"')
                    results.append((name, 'OK' if r == 0 else 'FAIL'))
                except Exception as e:
                    results.append((name, f'ERROR: {e}'))
            else:
                results.append((name, 'NOT FOUND'))
        print("\n" + "=" * 55)
        print("执行汇总:")
        for name, status in results:
            print(f"  [{status}] {name}")
    else:
        run_tool(cmd)


if __name__ == '__main__':
    main()