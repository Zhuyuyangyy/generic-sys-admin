# 用户操作手册

---

## 第一章 系统简介

### 1.1 系统概述

通用后台管理系统是一套基于SpringBoot3+Vue3的通用型后台管理解决方案，提供用户管理、角色权限、文件上传、TTS语音播报、数据可视化等功能模块。

### 1.2 系统架构

```
前端（Vue3）  →  Nginx  →  后端（SpringBoot3）
                            │
                      ┌─────┴─────┐
                      ↓           ↓
                   MySQL       MinIO
                  (主数据)    (文件存储)
```

---

## 第二章 环境准备

### 2.1 软件要求

| 软件 | 版本要求 | 下载地址 |
|------|----------|----------|
| JDK | 17或更高 | https://adoptium.net |
| Node.js | 18或更高 | https://nodejs.org |
| MySQL | 8.0或更高 | https://dev.mysql.com |
| Docker | 24或更高 | https://docker.com |
| Git | 最新版 | https://git-scm.com |

### 2.2 硬件要求

- CPU：双核及以上
- 内存：4GB及以上（推荐8GB）
- 硬盘：10GB可用空间

---

## 第三章 项目启动（Windows）

### 3.1 第一步：检查环境

双击运行项目根目录下的 `check_env.bat`，检查结果：

```
✅ JAVA_HOME: C:\Program Files\Eclipse Adoptium\jdk-17...
✅ MySQL端口3306: 可连接
✅ 后端端口8080: 空闲
✅ 前端端口5173: 空闲
```

如果有任何 ❌，请先安装/启动对应服务。

### 3.2 第二步：初始化数据库

1. 打开 Navicat 或 MySQL Workbench
2. 连接本地MySQL
3. 新建数据库：`generic_sys_admin`
4. 执行建表脚本：

```bash
# 在项目根目录执行
mysql -u root -p generic_sys_admin < sql/v1.0__init.sql
```

或在MySQL命令行中：
```sql
CREATE DATABASE IF NOT EXISTS generic_sys_admin;
USE generic_sys_admin;
SOURCE D:/ZYY\ Project/generic-sys-admin/sql/v1.0__init.sql;
```

### 3.3 第三步：启动后端

**方式一：IDE启动（推荐开发）**

1. 用IntelliJ IDEA或VS Code打开 `backend` 目录
2. 等待Maven依赖下载完成
3. 找到 `GenericSysAdminApplication.java`
4. 右键 → Run 'GenericSysAdminApplication'
5. 控制台看到以下内容表示启动成功：
```
Started GenericSysAdminApplication in X seconds
```

**方式二：命令行启动**

```bash
cd D:\ZYY Project\generic-sys-admin\backend
mvn spring-boot:run
```

### 3.4 第四步：启动前端

```bash
cd D:\ZYY Project\generic-sys-admin\frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173

### 3.5 第五步：启动MinIO（可选）

如需使用文件上传功能：

```bash
cd D:\ZYY Project\generic-sys-admin
docker-compose -f doc/startup/minio-docker-compose.yml up -d
```

MinIO控制台：http://localhost:9001（账号：minioadmin，密码：minioadmin）

---

## 第四章 登录与首页

### 4.1 登录系统

1. 打开 http://localhost:5173
2. 输入账号密码：

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 超级管理员 |
| user | 123456 | 普通用户 |

3. 点击"登录"按钮

### 4.2 首页说明

登录成功后进入首页，包含：

- **顶部导航栏**：Logo、项目名称、暗色模式切换、用户头像
- **左侧菜单栏**：根据用户角色动态显示可访问菜单
- **主内容区**：默认显示数据可视化大屏

---

## 第五章 用户管理模块

### 5.1 进入用户管理

路径：左侧菜单 → 系统管理 → 用户管理

### 5.2 查询用户

- **关键词搜索**：在搜索框输入用户名/昵称
- **分页**：底部可切换每页显示条数

### 5.3 新增用户

步骤：
1. 点击右上角"新增用户"按钮
2. 填写表单：
   - 用户名（必填，唯一）
   - 密码（必填）
   - 昵称
   - 邮箱
3. 点击"确认"保存

### 5.4 编辑用户

步骤：
1. 在用户列表找到目标用户
2. 点击操作列的"编辑"按钮
3. 修改信息后点击"确认"

### 5.5 删除用户

步骤：
1. 在用户列表找到目标用户
2. 点击操作列的"删除"按钮
3. 确认提示"是否确认删除？"

> 注意：删除为逻辑删除，数据仍保存在数据库中。

### 5.6 分配角色

步骤：
1. 点击操作列的"分配角色"按钮
2. 在弹出列表中勾选要分配的角色
3. 点击"确认"

---

## 第六章 角色管理模块

### 6.1 进入角色管理

路径：左侧菜单 → 系统管理 → 角色管理

### 6.2 角色列表

显示系统中所有角色信息（角色名、标识、描述、排序）

### 6.3 新增角色

步骤：
1. 点击"新增角色"按钮
2. 填写：角色名称、角色标识（英文唯一）、描述、排序
3. 点击"确认"

### 6.4 配置权限

步骤：
1. 点击操作列的"配置权限"按钮
2. 在菜单树中勾选该角色可访问的菜单
3. 点击"确认"

---

## 第七章 文件管理模块

### 7.1 进入文件管理

路径：点击任意页面的"上传文件"入口

### 7.2 上传文件

步骤：
1. 点击"上传文件"按钮
2. 选择本地文件（支持：jpg、png、gif、pdf、docx、xlsx等）
3. 文件自动上传，显示上传结果

> 注意：单个文件大小不超过10MB

### 7.3 查看上传结果

上传成功后：
- 显示文件访问URL
- 可直接复制URL
- 可点击删除文件

---

## 第八章 数据可视化大屏

### 8.1 进入大屏

路径：左侧菜单 → 数据大屏

### 8.2 指标卡片

页面顶部显示5个关键指标卡片：
- 用户总数
- 今日活跃
- 文件数量
- 系统评分
- 运行时间

### 8.3 趋势折线图

- 点击"周视图"：显示近7天趋势
- 点击"月视图"：显示近30天趋势
- 点击"年视图"：显示年度趋势

### 8.4 饼图分析

显示设备状态分布：
- 绿色：在线
- 蓝色：待机
- 灰色：离线

### 8.5 暗色模式

点击右上角🌙图标切换深色/浅色主题。

---

## 第九章 TTS语音播报

### 9.1 功能说明

系统集成Minimax TTS文字转语音功能，可在关键操作后自动播报。

### 9.2 使用场景

- 文件上传成功后："文件上传成功"
- 用户登录成功后："欢迎回来，管理员"
- 任务完成后："操作已完成"

### 9.3 Mock模式

若未配置Minimax API Key，系统自动进入Mock模式：
- 接口返回 `/mock/audio/xxx.mp3`
- 控制台输出彩色日志代替真实语音
- 不影响其他功能正常运行

---

## 第十章 系统日志

### 10.1 查看日志

路径：左侧菜单 → 系统管理 → 操作日志

显示所有用户的关键操作记录。

### 10.2 日志内容

| 字段 | 说明 |
|------|------|
| 用户名 | 操作人 |
| 操作内容 | 操作描述 |
| IP地址 | 操作者IP |
| 耗时 | 执行时间（毫秒） |
| 状态 | 成功/异常 |
| 时间 | 操作时间 |

---

## 第十一章 常见问题

### Q1：启动时报错"JAVA_HOME not found"

**解决方法**：
1. 确认已安装JDK 17+
2. 设置环境变量：`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17...`
3. 重启命令行窗口

### Q2：前端npm install很慢

**解决方法**：
```bash
# 使用淘宝镜像
npm install --registry=https://registry.npmmirror.com
```

### Q3：数据库连接失败

**解决方法**：
1. 确认MySQL服务已启动
2. 检查 `application-dev.yml` 中的数据库配置
3. 确认用户名密码正确

### Q4：MinIO无法访问

**解决方法**：
```bash
# 重启MinIO容器
docker-compose -f doc/startup/minio-docker-compose.yml restart

# 或检查端口占用
netstat -an | findstr 9000
```

### Q5：接口返回401未认证

**解决方法**：
1. 确认Token未过期（有效期2小时）
2. 重新登录获取新Token
3. 检查请求头是否正确携带Token

---

## 附录：默认账号密码

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 超级管理员 | admin | 123456 | 全部权限 |
| 普通用户 | user | 123456 | 受限权限 |

> ⚠️ 正式部署时请立即修改默认密码！

---

*文档版本：v1.0*
*最后更新：2026-04-19*
