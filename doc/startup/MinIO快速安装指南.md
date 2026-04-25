# MinIO 快速安装指南

## 📖 什么是 MinIO？

MinIO 是一款高性能、分布式对象存储系统，**与 AWS S3 完全兼容**。

### 对比传统本地存储

| 维度 | 本地磁盘存储 | MinIO 分布式存储 |
|------|------------|----------------|
| 容量 | 受限单机磁盘 | 集群扩容，EB 级 |
| 可靠性 | 单点故障 | 多副本冗余，零丢失 |
| 访问 | 本机访问 | HTTP API，任意终端 |
| 部署 | 手动挂载 | **一条命令完成** |
| CDN | 不支持 | 支持，配合 Nginx |

---

## 🚀 方法一：Docker 一键启动（推荐）

### Step 1：确认 Docker 已安装

```bash
docker --version
# Docker version 26.0.0, build 2ae903e
```

> **没有安装？** Windows: 下载 [Docker Desktop](https://www.docker.com/products/docker-desktop/) | macOS: 同左 | Linux: `curl -fsSL https://get.docker.com | bash`

### Step 2：一键启动

在项目根目录执行：

```bash
cd D:\ZYY Project\generic-sys-admin\doc\startup
docker-compose -f minio-docker-compose.yml up -d
```

### Step 3：验证启动成功

```bash
# 查看容器状态
docker-compose -f minio-docker-compose.yml ps

# 应该看到：
# NAME              STATUS
# minio-server      Up (healthy)
# minio-init        Exited (0)
```

### Step 4：打开控制台

浏览器访问：**http://localhost:9001**

```
用户名：minioadmin
密码：minioadmin
```

### Step 5：连接后端

在 `application-dev.yml` 中确认配置：

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: generic-sys-admin
```

---

## 🚀 方法二：Windows 原生安装（无需 Docker）

### Step 1：下载 MinIO Server

浏览器下载：https://dl.min.io/server/minio/release/windows-amd64/minio.exe

或者用 PowerShell：

```powershell
Invoke-WebRequest -Uri "https://dl.min.io/server/minio/release/windows-amd64/minio.exe" -OutFile "C:\minio\minio.exe"
```

### Step 2：创建数据目录

```powershell
mkdir C:\minio\data
mkdir C:\minio\config
```

### Step 3：启动 MinIO

```powershell
cd C:\minio
.\minio.exe server C:\minio\data --console-address ":9001"
```

### Step 4：保持后台运行（可选）

用 **NSSM** 或 **WinSW** 将 MinIO 注册为 Windows 服务。

---

## 🧪 快速验证（mc 命令行）

### 安装 MinIO Client（mc）

```bash
docker run -it --entrypoint=sh minio/mc
```

### 连接本地 MinIO

```bash
mc alias set myminio http://localhost:9000 minioadmin minioadmin
```

### 查看 Bucket

```bash
mc ls myminio/
```

### 上传一个测试文件

```bash
# 先创建一个测试文件
echo "Hello MinIO" > test.txt

# 上传到 Bucket
mc cp test.txt myminio/generic-sys-admin/

# 获取访问 URL
mc share download myminio/generic-sys-admin/test.txt
```

---

## 🔧 常用运维命令

### 查看 MinIO 日志

```bash
docker logs minio-server
```

### 进入 MinIO 容器内部

```bash
docker exec -it minio-server sh
```

### 重启 MinIO

```bash
docker-compose -f minio-docker-compose.yml restart minio
```

### 停止 MinIO

```bash
docker-compose -f minio-docker-compose.yml stop minio
```

### 完全删除（包括数据）

```bash
docker-compose -f minio-docker-compose.yml down -v
# ⚠️ 警告：这会删除所有存储的文件！
```

---

## 🌐 前端如何访问文件？

MinIO 设置了**公开只读策略**，前端可以直接用 URL 访问文件：

```html
<!-- 直接显示图片，无需签名 -->
<img src="http://localhost:9000/generic-sys-admin/abc123.jpg" />
```

```javascript
// 上传成功后返回的 URL
const fileUrl = "http://localhost:9000/generic-sys-admin/f7c3a8b1.png"

// 前端直接使用
this.avatar = fileUrl
```

---

## 🔐 生产环境安全配置

### 1. 修改默认密码

在 `docker-compose.yml` 中修改：

```yaml
environment:
  MINIO_ROOT_USER: your-secure-username
  MINIO_ROOT_PASSWORD: your-secure-password!!
```

### 2. 使用 Nginx 反向代理

```nginx
# /etc/nginx/conf.d/minio.conf
server {
    listen 9000;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. 开启 HTTPS（SSL）

在 Nginx 层配置证书：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/your-domain.crt;
    ssl_certificate_key /etc/ssl/private/your-domain.key;

    location / {
        proxy_pass http://127.0.0.1:9000;
    }
}
```

---

## ❓ 常见问题

### Q: 端口 9000 / 9001 被占用？

修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "9002:9000"   # API 改到 9002
  - "9003:9001"   # Console 改到 9003
```

然后更新 `application-dev.yml`：

```yaml
minio:
  endpoint: http://localhost:9002
```

### Q: 忘记密码？

```bash
# 进入容器
docker exec -it minio-server sh

# 重置密码（需要停服）
mc admin user add myminio newadmin newpassword
```

### Q: 上传文件报 403 Forbidden？

这是因为 MinIO 刚启动时 Bucket 还没初始化完成。
`minio-init` 容器会自动设置公开只读策略。

如果仍然报错，检查 Policy：

```bash
docker exec -it minio-server sh
mc anonymous get myminio/generic-sys-admin
```

### Q: 如何查看存储桶里的文件？

```bash
mc ls myminio/generic-sys-admin/
```

---

**💡 提示：开发阶段用 Docker 最省心，配置文件一个不改，直接运行即可！**
