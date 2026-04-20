# Generic Sys Admin - 通用管理系统

> 通用设备与耗材管理系统，支持设备管理、耗材管理、AI对话、语音合成等模块。

## 📋 功能模块

| 模块 | 说明 |
|------|------|
| 🔐 用户管理 | 用户增删改查、角色分配、密码重置 |
| 📁 角色管理 | RBAC 角色权限体系 |
| 🏠 菜单管理 | 前端动态菜单配置 |
| 🔧 设备管理 | 设备信息、状态流转、维护记录 |
| 📦 耗材管理 | 库存管理、入库/出库、库存预警 |
| 🤖 AI工作室 | 对话、语音合成（TTS）、多模态 AI |
| 📝 操作日志 | 完整审计日志 |

## 🏗️ 技术栈

### 后端
- **Java 17** + **Spring Boot 3.4**
- **MyBatis-Plus**（ORM）
- **Spring Security** + **JWT**（认证授权）
- **Redis**（缓存，Optional）
- **Knife4j**（API 文档）
- **Lombok**（简化代码）

### 前端
- **Vue 3** + **TypeScript**
- **Vite**（构建工具）
- **Pinia**（状态管理）
- **Element Plus**（UI 组件库）
- **Axios**（HTTP 客户端）

## 🚀 本地开发

### 前置条件

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis（可选，无 Redis 时系统自动降级）

### 1. 初始化数据库

```sql
-- 创建数据库（UTF8MB4）
CREATE DATABASE generic_sys_admin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 执行初始化脚本
SOURCE sql/v1.0__init.sql;
```

### 2. 启动后端

```bash
cd backend

# 配置数据库密码（可选，有默认值）
# Windows:
set DB_PASSWORD=1234
set DB_HOST=localhost

# Linux/Mac:
export DB_PASSWORD=1234
export DB_HOST=localhost

# 启动（开发模式，自动使用 dev profile）
mvn spring-boot:run

# 或指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动地址：http://localhost:8081
Knife4j 文档：http://localhost:8081/doc.html

### 3. 启动前端

```bash
cd frontend

# 安装依赖（首次）
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

前端访问地址：http://localhost:5173

### 4. 登录账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 操作员 | operator | admin123 |
| 查看者 | viewer | admin123 |

## 🐳 Docker 部署（推荐）

### 一键启动

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env，修改以下必填项：
# MYSQL_ROOT_PASSWORD=your_strong_password
# JWT_SECRET=your_very_long_random_secret_key_at_least_32_chars

# 启动全部服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

部署完成后：
- 前端：http://localhost
- 后端 API：http://localhost:8081
- API 文档：http://localhost:8081/doc.html

### 停止服务

```bash
docker-compose down

# 同时删除数据卷（清空数据库！）
docker-compose down -v
```

## 📁 目录结构

```
generic-sys-admin/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/zyy/
│   │   ├── controller/         # REST 控制器
│   │   ├── service/           # 业务逻辑
│   │   ├── mapper/            # MyBatis Mapper
│   │   ├── model/             # DTO/VO/Entity
│   │   ├── config/            # 配置类
│   │   ├── security/          # 安全相关
│   │   ├── aspect/            # AOP 切面
│   │   ├── enums/             # 枚举常量
│   │   └── exception/         # 自定义异常
│   ├── src/main/resources/
│   │   ├── application.yml    # 主配置
│   │   ├── application-dev.yml # 开发配置
│   │   ├── application-prod.yml # 生产配置
│   │   └── logback-spring.xml  # 日志配置
│   ├── sql/
│   │   └── v1.0__init.sql    # 数据库初始化脚本
│   └── Dockerfile
│
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/               # API 接口封装
│   │   ├── components/        # 公共组件
│   │   ├── views/             # 页面视图
│   │   ├── store/             # Pinia 状态
│   │   ├── router/            # 路由配置
│   │   ├── utils/             # 工具函数
│   │   └── config/            # 页面配置
│   ├── nginx.conf             # Nginx 配置
│   └── Dockerfile
│
├── docker-compose.yml          # 容器编排
└── README.md
```

## 🔧 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_HOST` | 数据库地址 | localhost |
| `DB_PORT` | 数据库端口 | 3306 |
| `DB_NAME` | 数据库名 | generic_sys_admin |
| `DB_USERNAME` | 数据库用户名 | root |
| `DB_PASSWORD` | 数据库密码 | （必填） |
| `REDIS_HOST` | Redis 地址 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | 空 |
| `JWT_SECRET` | JWT 签名密钥（生产必填！） | dev 默认值 |
| `MINIMAX_API_KEY` | MiniMax API Key（AI 功能） | 空 |
| `DOC_ENABLED` | 是否开启 API 文档 | true（dev）/ false（prod） |

## 🔐 安全说明

- ⚠️ 生产环境务必修改 `JWT_SECRET` 为至少 32 位的随机字符串
- ⚠️ 生产环境务必修改数据库密码
- ⚠️ 生产环境限制 `DOC_ENABLED=false`
- ⚠️ 前端反向代理仅适用于开发环境，生产环境建议 Nginx 独立部署

## 📄 许可证

Apache License 2.0
