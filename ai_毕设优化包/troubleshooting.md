# 常见问题排查手册

---

## 一、后端启动问题

### 问题1：JAVA_HOME未设置

**错误信息**：
```
Error: JAVA_HOME is not set
```

**解决方案**：

Windows：
```bat
# 临时设置（当前命令行窗口有效）
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x

# 永久设置（系统环境变量）
# 右键此电脑 → 属性 → 高级系统设置 → 环境变量 → 新建系统变量
# 变量名：JAVA_HOME
# 变量值：C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x
```

Linux：
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

---

### 问题2：端口被占用

**错误信息**：
```
Web server failed to start. Port 8080 was already in use.
```

**解决方案**：

```bash
# Windows查找占用端口的进程
netstat -ano | findstr :8080
# 返回类似：TCP    0.0.0.0:8080    0.0.0.0:0    LISTENING    12345
# 结束进程
taskkill /PID 12345 /F

# Linux
lsof -i :8080
kill -9 <PID>
```

**常用端口对照**：

| 端口 | 服务 | 常见问题 |
|------|------|----------|
| 3306 | MySQL | 未启动、密码错误 |
| 5173 | 前端Vite | 已被占用 |
| 8080 | 后端SpringBoot | 已被占用 |
| 9000 | MinIO API | 未启动 |
| 9001 | MinIO Console | 未启动 |

---

### 问题3：数据库连接失败

**错误信息**：
```
Connection refused. Verify the connection string.
```

**排查步骤**：

1. 确认MySQL服务已启动
```bash
# Windows
net start | findstr MySQL

# Linux
systemctl status mysql
```

2. 检查连接信息
```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf-8
    username: root
    password: your_password  # 确认密码正确
```

3. 测试连接
```bash
mysql -u root -p123456 -e "SELECT 1;"
```

---

### 问题4：Maven依赖下载失败

**错误信息**：
```
Could not resolve dependencies: ... Failed to collect dependencies
```

**解决方案**：

```bash
# 清理Maven缓存
mvn dependency:purge-local-repository
mvn clean install -U

# 或使用阿里云镜像（编辑~/.m2/settings.xml）
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>*</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

## 二、前端启动问题

### 问题5：npm install失败

**错误信息**：
```
npm ERR! network request to https://registry.npmjs.org/... failed
```

**解决方案**：

```bash
# 使用淘宝镜像
npm install --registry=https://registry.npmmirror.com

# 或永久设置
npm config set registry https://registry.npmmirror.com
```

---

### 问题6：Vite启动报错

**错误信息**：
```
Error: cannot find module 'vue'
```

**解决方案**：

```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

---

### 问题7：跨域问题（生产环境）

**错误信息**：
```
Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:5173' 
has been blocked by CORS policy
```

**解决方案**：

确保Nginx配置了正确的代理：
```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

---

## 三、MinIO存储问题

### 问题8：MinIO启动失败

**错误信息**：
```
mkdir: cannot create directory '/data': Permission denied
```

**解决方案**：

```bash
# 创建数据目录并授权
sudo mkdir -p /data/minio
sudo chown -R $USER:$USER /data/minio

# 或使用Docker运行（自动处理权限）
docker run -d -p 9000:9000 -p 9001:9001 \
  --name minio \
  -v /data/minio:/data \
  minio/minio server /data --console-address ":9001"
```

---

### 问题9：文件上传返回403

**可能原因**：
1. MinIO Bucket Policy未正确设置
2. 文件类型不在白名单内

**解决方案**：

1. 检查MinIO Bucket Policy是否为公开读
2. 检查 `application-dev.yml` 中的 `file.whitelist-types`

---

### 问题10：MinIO连接超时

**解决方案**：

```bash
# 检查MinIO服务状态
docker ps | grep minio

# 重启MinIO
docker-compose -f doc/startup/minio-docker-compose.yml restart

# 检查端口
netstat -an | findstr 9000
```

---

## 四、TTS语音问题

### 问题11：TTS接口返回500

**错误信息**：
```
Failed to invoke TTS service: API key is not configured
```

**这是正常现象**，表示系统正常运行在Mock模式下。

**解决方案**：

如需真实TTS功能，配置环境变量：
```bash
export MINIMAX_API_KEY=your_api_key_here
```

Mock模式下的日志输出：
```
[MockTTS] 🎙️ 语音合成（模拟）| 文本: 文件上传成功 | 音色: male-qn-qingse
```

---

## 五、功能异常问题

### 问题12：JWT Token无效

**错误信息**：
```
JWT validation failed: Invalid signature
```

**可能原因**：
1. JWT Secret被修改
2. Token过期（默认2小时）

**解决方案**：
1. 重新登录获取新Token
2. 检查 `jwt.secret` 配置是否一致

---

### 问题13：用户删除后仍能登录

**原因**：使用了逻辑删除（is_deleted），数据未真正从数据库删除

**解决方案**：

如需彻底删除用户，使用物理删除SQL：
```sql
DELETE FROM sys_user_role WHERE user_id = <id>;
DELETE FROM sys_user WHERE id = <id>;
```

---

### 问题14：菜单不显示

**排查步骤**：

1. 检查用户是否有对应角色
```sql
SELECT * FROM sys_user_role WHERE user_id = <user_id>;
```

2. 检查角色是否有菜单权限
```sql
SELECT * FROM sys_role_menu WHERE role_id = <role_id>;
```

3. 检查sys_menu表数据是否完整

---

## 六、性能问题

### 问题15：接口响应慢

**排查方向**：
1. MySQL慢查询 → 添加索引
2. MySQL连接池不足 → 调整 `spring.datasource.hikari.maximum-pool-size`
3. MinIO上传慢 → 检查网络带宽

**索引优化示例**：
```sql
-- 日志表添加索引
CREATE INDEX idx_create_time ON sys_operation_log(create_time);
CREATE INDEX idx_username ON sys_operation_log(username);
```

---

## 七、其他问题

### 问题16：热部署不生效（开发环境）

**解决方案**：
```bash
# 使用spring-boot-devtools
# 确保IDE开启了自动编译
# 重启后端服务
```

---

### 问题17：日志乱码

**解决方案**：

修改 `logback-spring.xml`：
```xml
<property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"/>
```

确保IDE控制台编码为UTF-8。

---

## 快速诊断命令清单

```bash
# 1. 检查Java
java -version

# 2. 检查Node
node -v

# 3. 检查MySQL
mysql -u root -p -e "SELECT VERSION();"

# 4. 检查端口占用
netstat -ano | findstr "3306 5173 8080 9000 9001"

# 5. 检查Docker容器
docker ps -a | grep -E "mysql|minio"

# 6. 查看后端日志
tail -f backend/logs/spring.log

# 7. 重启后端（Windows）
taskkill /F /IM java.exe
cd backend && mvn spring-boot:run
```

---

*文档版本：v1.0*
*最后更新：2026-04-19*
