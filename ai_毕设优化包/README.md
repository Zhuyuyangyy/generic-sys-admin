# 通用后台管理系统 - AI毕设优化包

> 本目录包含通用后台管理系统项目的全套毕设文档、部署脚本和优化报告。

---

## 📁 目录结构

```
ai_毕设优化包/
│
├── 📄 00_优化完成报告.md      # 优化任务执行报告
│
├── 📄 requirements.md         # 项目需求分析说明书
│
├── 📄 system_design.md       # 系统设计说明书
│
├── 📄 database_design.md     # 数据库设计文档（含完整SQL）
│
├── 📄 test_report.md         # 系统测试报告
│
├── 📄 user_manual.md          # 用户操作手册
│
├── 📄 thesis_draft.md         # 毕设论文初稿（完整格式）
│
├── 📄 defense_ppt.md          # 答辩PPT大纲（12页）
│
├── 📄 deployment_guide.md    # 一键部署指南
│
├── 📄 troubleshooting.md      # 常见问题排查手册
│
├── 📄 demo_script.md          # 项目演示脚本（答辩用）
│
├── 📄 changelog.md            # 版本更新记录
│
├── 📄 architecture.md         # 架构设计文档（图解）
│
├── 📄 project_summary.md      # 项目总结报告
│
└── 📁 deploy/                # 部署脚本目录
    ├── windows_deploy.bat     # Windows一键部署脚本
    └── linux_deploy.sh       # Linux一键部署脚本
```

---

## 🎯 核心亮点（5个创新点）

| # | 创新点 | 技术实现 | 答辩话术 |
|---|--------|----------|----------|
| 1 | JWT无状态RBAC认证 | JwtAuthFilter + 动态菜单 | "无状态认证，分布式友好" |
| 2 | Minimax TTS语音播报 | TtsService接口 + Mock降级 | "智能交互，Mock免Key调试" |
| 3 | AOP操作日志审计 | @Aspect + @Around切面 | "零侵入，全自动记录" |
| 4 | ECharts + 3D可视化 | Three.js + 降级几何体 | "数据驱动，酷炫展示" |
| 5 | MinIO分布式对象存储 | 策略模式 + 自动降级 | "S3兼容，一条命令启动" |

---

## 📋 文档使用指南

### 论文写作

1. 打开 `thesis_draft.md` 作为初稿
2. 根据实际项目细节调整内容
3. 补充实验数据、截图、代码注释
4. 格式按学校要求调整

### 答辩准备

1. 阅读 `defense_ppt.md` 作为PPT制作参考
2. 按 `demo_script.md` 演练演示流程
3. 准备 `troubleshooting.md` 中的QA答案
4. 把 `architecture.md` 打印带去答辩现场

### 项目部署

1. 查看 `deployment_guide.md` 选择部署方式
2. 使用对应脚本快速部署
3. 遇到问题查阅 `troubleshooting.md`

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Docker 24+（可选）

### 启动命令

```bash
# 1. 初始化数据库
mysql -u root -p < sql/v1.0__init.sql

# 2. 启动后端
cd backend
mvn spring-boot:run

# 3. 启动前端（新窗口）
cd frontend
npm install
npm run dev
```

访问：
- 前端：http://localhost:5173
- 后端：http://localhost:8080
- 文档：http://localhost:8080/doc.html

---

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| Java源文件 | 33个 |
| 前端文件 | 12个 |
| 总代码量 | ~25,000行 |
| 毕设文档 | 14份 |
| 测试用例 | 39个 |
| 功能模块 | 7个 |

---

## ⚠️ 注意事项

1. **密码安全**：默认账号 admin/123456，正式部署请立即修改
2. **API Key**：Minimax API Key通过环境变量注入，不要硬编码
3. **跨域配置**：生产环境务必通过Nginx反向代理，不要直接暴露后端
4. **数据备份**：重要数据请定期备份，MySQL建议开启binlog

---

*本优化包由AI辅助生成，内容基于项目实际代码编写*
*最后更新：2026-04-19*
