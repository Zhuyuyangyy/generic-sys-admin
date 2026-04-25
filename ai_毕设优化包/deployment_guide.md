# 一键部署指南

---

## Windows环境部署

### 方式一：自动化脚本部署

运行项目根目录下的 `deploy\windows_deploy.bat`：

```bat
cd D:\ZYY Project\generic-sys-admin
deploy\windows_deploy.bat
```

脚本会自动：
1. 检查Java、Node.js、MySQL环境
2. 初始化数据库
3. 启动后端服务
4. 安装前端依赖
5. 启动前端服务

### 方式二：手动分步部署

#### 第一步：环境检查

```bash
# 检查Java版本（需要17+）
java -version

# 检查Node.js版本（需要18+）
node -v

# 检查MySQL
mysql --version
```

#### 第二步：初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS generic_sys_admin CHARACTER SET utf8mb4;"
mysql -u root -p generic_sys_admin < sql/v1.0__init.sql
```

#### 第三步：配置后端

编辑 `backend/src/main/resources/application-dev.yml`，确保以下配置正确：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf-8
    username: root
    password: your_password

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: generic-sys-admin
```

#### 第四步：启动后端

```bash
cd backend
mvn spring-boot:run
```

或打包后运行：
```bash
mvn clean package -DskipTests
java -jar target/generic-sys-admin-1.0.0.jar --spring.profiles.active=prod
```

#### 第五步：启动前端

```bash
cd frontend
npm install
npm run dev
```

---

## Linux环境部署

### 第一步：安装依赖

```bash
# CentOS/RHEL
sudo yum install -y java-17-openjdk java-17-openjdk-devel
sudo yum install -y nodejs npm
sudo yum install -y mysql-server

# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-17-jdk nodejs npm
sudo apt install -y mysql-server
```

### 第二步：初始化数据库

```bash
mysql -u root -p < sql/v1.0__init.sql
```

### 第三步：启动后端

```bash
cd backend
nohup mvn spring-boot:run > logs/backend.log 2>&1 &
echo "Backend PID: $!"
```

### 第四步：启动前端

```bash
cd frontend
npm install --registry=https://registry.npmmirror.com
nohup npm run dev > logs/frontend.log 2>&1 &
echo "Frontend PID: $!"
```

---

## Docker环境部署（推荐生产环境）

### 使用docker-compose一键启动

```bash
cd D:\ZYY Project\generic-sys-admin

# 启动MySQL
docker-compose -f doc/startup/mysql-docker-compose.yml up -d

# 启动MinIO
docker-compose -f doc/startup/minio-docker-compose.yml up -d

# 等待30秒让MySQL初始化完成

# 初始化数据库
docker exec -i mysql-container mysql -uroot -p123456 < sql/v1.0__init.sql
```

### 构建并启动应用

```bash
# 构建后端JAR
cd backend
mvn clean package -DskipTests

# 使用Dockerfile构建镜像（需创建Dockerfile）
docker build -t generic-sys-admin:latest .
docker run -d -p 8080:8080 --name gsa-backend generic-sys-admin:latest
```

---

## Nginx生产部署配置

### 1. 打包前端

```bash
cd frontend
npm run build
# 产物在 dist/ 目录
```

### 2. Nginx配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/generic-sys-admin/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # MinIO静态文件（如果不用CDN）
    location /local-files/ {
        alias /data/uploads/;
        expires 7d;
    }
}
```

### 3. SSL配置（Let's Encrypt免费证书）

```bash
sudo certbot --nginx -d your-domain.com
```

---

## 环境变量说明（生产环境）

### 后端环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| JAVA_HOME | JDK路径 | /usr/lib/jvm/java-17 |
| MINIMAX_API_KEY | Minimax API密钥 | eyJh... |
| MINIMAX_APP_ID | Minimax应用ID | 1234567 |
| MINIMAX_GROUP_ID | Minimax分组ID | 9876543 |
| MINIO_ACCESS_KEY | MinIO访问密钥 | minioadmin |
| MINIO_SECRET_KEY | MinIO密钥 | minioadmin |
| JWT_SECRET | JWT签名密钥 | 自定义长字符串 |

### 前端环境变量（.env.production）

```bash
VITE_API_BASE_URL=/api
```
