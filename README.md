# 通用管理系统母版 (Generic Sys Admin)

基于 SpringBoot3 + Vue3 的高复用、易扩展通用管理系统母版，专为软件杯等竞赛设计。

## 🎯 项目简介

本项目为**通用管理系统母版**，提供完整的前后端架构、RBAC权限控制、AOP日志审计、数据可视化大屏等企业级功能。开发人员可在次母版基础上快速构建面向不同业务场景的管理系统。

## 🏗️ 技术架构

| 层级 | 技术选型 |
|------|---------|
| 后端 | SpringBoot3 + MyBatis-Plus + JWT + MySQL |
| 前端 | Vue3 + Element Plus + Pinia + ECharts |
| 扩展 | Minimax TTS 语音播报 |

## ✨ 五大创新点

1. **基于JWT的无状态RBAC动态权限控制模型**
2. **融合Minimax TTS的智能语音播报交互**
3. **基于AOP切面编程的系统级操作日志审计**
4. **结合ECharts与轻量级3D渲染（WebGL）的动态数据可视化大屏**
5. **基于响应式原理的跨终端自适应UI架构**

## 📂 目录结构

```
generic-sys-admin/
├── sql/                    # 数据库脚本
├── doc/                    # 项目文档
│   ├── thesis/            # 毕业论文大纲
│   ├── ppt/               # 答辩PPT大纲
│   └── startup/           # 本地启动说明
├── backend/               # SpringBoot后端
│   └── src/main/
│       ├── java/com/zyy/
│       │   ├── common/    # 统一返回、异常处理
│       │   ├── security/  # JWT认证
│       │   ├── aspect/     # AOP日志切面
│       │   ├── rbac/       # 用户角色菜单实体
│       │   ├── voice/      # TTS语音
│       │   └── exception/  # 业务异常
│       └── resources/
│           └── application.yml  # 后端配置
└── frontend/               # Vue3前端
    └── src/
        ├── api/            # Axios封装
        ├── components/     # 通用组件
        │   └── chart/      # ECharts/Three.js图表
        └── views/          # 页面
```

## 🔐 安全提醒

### ⚠️ Minimax API Key 配置

交付源码时，**必须**提醒客户在 `backend/src/main/resources/application.yml` 中填入自己的密钥：

```yaml
minimax:
  api-url: https://api.minimax.chat
  app-id: 你的AppId           # ⚠️ 替换为实际值
  api-key: ${MINIMAX_API_KEY} # ⚠️ 通过环境变量注入，不要硬编码！
  group-id: ${MINIMAX_GROUP_ID} # ⚠️ 同上
```

切勿将真实的 `api-key` 提交到代码仓库！

## 🚀 快速启动

详细步骤请参考 [本地启动说明](doc/startup/本地启动说明.md)

## 🌐 部署说明

### 开发环境
前端 Vite 已配置 `/api` 代理到 `http://localhost:8080`，前端直接用 `npm run dev` 启动即可。

### 生产环境 ⚠️（重要）
前端打包后需要配置 **Nginx 反向代理**，否则会遇到 **403 跨域错误**：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

> ⚠️ 如果不配Nginx直接部署dist目录，浏览器会报403！这是最常见的部署售后问题。

## 🎨 3D组件扩展

`frontend/src/components/chart/Echarts3D.vue` 提供了基础的 Three.js 3D地球组件。

如需接入**医疗器械、牙科机器人**等3D模型：

1. 将 `.glb` / `.gltf` 模型文件放入 `frontend/public/models/`
2. 修改组件中的模型路径即可：
```javascript
const loader = new GLTFLoader()
loader.load('/models/your-dental-robot.glb', (gltf) => {
    scene.add(gltf.scene)
})
```

## 📋 数据库表

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表 |
| `sys_role` | 角色表 |
| `sys_user_role` | 用户角色关联表 |
| `sys_menu` | 菜单权限表 |
| `sys_role_menu` | 角色菜单关联表 |
| `sys_operation_log` | 操作日志表 |
| `template_entity` | 通用模板实体表 |

## 🔐 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | admin | 123456 |
| 普通用户 | user | 123456 |

> ⚠️ 正式环境请务必修改默认密码！

## 📚 相关文档

- [毕业论文大纲](doc/thesis/毕业论文大纲.md)
- [答辩PPT大纲](doc/ppt/答辩PPT大纲.md)
- [本地启动说明](doc/startup/本地启动说明.md)
- [数据库初始化脚本](sql/v1.0__init.sql)

## 🏆 赛题适配

本项目专为**第十五届中国软件杯 A3赛题**设计，可作为"基于大模型的个性化资源生成与学习多智能体系统"的通用管理后台框架。

---

> 子曰："工欲善其事，必先利其器。" —— 《论语·卫灵公》
