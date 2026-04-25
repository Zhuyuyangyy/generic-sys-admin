# 系统设计说明书

---

## 第一章 系统架构设计

### 1.1 总体架构

本系统采用**前后端分离架构**，后端提供RESTful API，前端基于Vue3构建单页应用。

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户浏览器                                 │
│              (Chrome / Firefox / Edge / Safari)                  │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP/HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Nginx 反向代理服务器                         │
│            (生产环境：负载均衡 + 静态资源托管 + API路由)              │
└────────────────────────┬────────────────────────────────────────┘
                         │
          ┌──────────────┴──────────────┐
          │                             │
          ▼                             ▼
┌─────────────────────┐   ┌─────────────────────┐
│   Vue3 前端工程       │   │  SpringBoot3 后端   │
│   (localhost:5173)   │   │  (localhost:8080)   │
│                      │   │                     │
│  ┌─────────────────┐ │   │  ┌───────────────┐  │
│  │  Element Plus   │ │   │  │ Controller层  │  │
│  │  ECharts 图表   │ │   │  │ Service层      │  │
│  │  Three.js 3D    │ │   │  │ Mapper层(MP)   │  │
│  │  Pinia 状态管理  │ │   │  └───────────────┘  │
│  │  Axios 请求封装  │ │   │         │            │
│  └─────────────────┘ │   │         ▼            │
│         │            │   │  ┌───────────────┐  │
│         ▼            │   │  │  MySQL 数据库  │  │
│  ┌─────────────────┐ │   │  │  MinIO 存储    │  │
│  │  Vite 打包构建  │ │   │  │  (对象存储)     │  │
│  └─────────────────┘ │   │  └───────────────┘  │
└─────────────────────┘   └─────────────────────┘
```

### 1.2 技术架构分层

```
┌────────────────────────────────────────────────────┐
│                   【表现层】Vue3 前端                 │
│  Element Plus 组件库 │ ECharts 图表 │ Three.js 3D  │
├────────────────────────────────────────────────────┤
│                   【网关层】Nginx                    │
│         反向代理 │ SSL终结 │ 静态资源 │ 负载均衡     │
├────────────────────────────────────────────────────┤
│                   【聚合层】Controller               │
│       @RestController 统一返回 Result<T>             │
├────────────────────────────────────────────────────┤
│                   【业务层】Service                  │
│  @Transactional事务 │ 业务逻辑 │ 调用Mapper/工具类    │
├────────────────────────────────────────────────────┤
│                   【数据层】Mapper + MyBatis-Plus   │
│        LambdaQueryWrapper │ 自动SQL生成 │ 逻辑删除   │
├────────────────────────────────────────────────────┤
│                   【基础设施层】                      │
│     MySQL(主数据) │ MinIO(文件) │ JWT(认证) │ 日志    │
└────────────────────────────────────────────────────┘
```

### 1.3 技术选型说明

| 层次 | 技术选型 | 版本 | 选型理由 |
|------|----------|------|----------|
| 后端框架 | SpringBoot3 | 3.2.x | Java17+，Jakarta EE9，响应式编程支持 |
| ORM框架 | MyBatis-Plus | 3.5.x | 强大的条件构造器，减少70%SQL代码量 |
| 认证方案 | JWT | - | 无状态认证，适合分布式/微服务架构 |
| 前端框架 | Vue3 | 3.4.x | Composition API，更好的TypeScript支持 |
| UI组件库 | Element Plus | 2.5.x | Vue3官方推荐，企业级组件丰富 |
| 状态管理 | Pinia | 2.1.x | Vue3官方推荐，比Vuex更轻量 |
| 图表库 | ECharts | 5.4.x | 国产开源，功能强大，文档完善 |
| 3D引擎 | Three.js | 0.160.x | WebGL标准库，社区活跃 |
| 对象存储 | MinIO | RELEASE-2024 | S3兼容，Docker一键部署，支持分布式 |

---

## 第二章 模块设计

### 2.1 后端模块划分

```
com.zyy
├── GenericSysAdminApplication.java    # SpringBoot启动类
├── config/                            # 配置层
│   ├── Knife4jConfig.java             # Swagger文档配置
│   ├── MinioConfig.java               # MinIO存储配置
│   ├── MinioAvailability.java         # MinIO可用性状态
│   ├── LocalFileStorageStrategy.java # 本地存储降级策略
│   └── MyBatisPlusConfig.java         # MyBatis-Plus配置
├── common/                            # 通用层
│   ├── BaseEntity.java                # 实体基类（雪花ID+逻辑删除）
│   ├── BaseController.java            # Controller基类
│   ├── Result.java                    # 统一响应结构
│   ├── ResultCode.java                # 统一错误码
│   ├── GlobalExceptionHandler.java     # 全局异常处理
│   └── PageParam.java                  # 分页参数
├── controller/                        # 控制层
│   ├── FileController.java            # 文件上传/下载
│   └── voice/
│       └── TtsController.java          # TTS语音接口
├── service/                          # 业务层（扩展点）
├── mapper/                            # 数据层
├── rbac/                              # 权限模型
│   ├── model/
│   │   ├── SysUser.java
│   │   ├── SysRole.java
│   │   └── SysMenu.java
│   └── (Service+Mapper)
├── voice/                             # 语音模块
│   ├── TtsService.java                # TTS服务接口
│   ├── MinimaxTtsUtil.java            # Minimax实现
│   └── MockTtsServiceImpl.java        # Mock实现（降级）
├── util/                              # 工具层
│   ├── MinioUtil.java                 # MinIO操作工具
│   ├── EasyExcelUtil.java            # Excel导出工具
│   └── JsonUtil.java                  # JSON处理工具
├── security/                          # 安全层
│   ├── JwtUtil.java                   # JWT工具
│   ├── JwtAuthFilter.java             # JWT认证过滤器
│   └── LoginUser.java                 # 登录用户信息
├── aspect/                            # 切面层
│   └── OperationLogAspect.java        # AOP日志记录
└── exception/                         # 异常层
    ├── BusinessException.java
    ├── UnauthorizedException.java
    ├── ForbiddenException.java
    └── ResourceNotFoundException.java
```

### 2.2 前端模块划分

```
frontend/src/
├── api/                              # 接口层
│   ├── axios.ts                       # Axios封装（拦截器）
│   ├── request.ts                      # 带Token的请求封装
│   └── voice/
│       └── tts.ts                      # TTS接口调用
├── components/                        # 组件层
│   ├── common/
│   │   ├── CrudTemplate.vue           # 通用CRUD模板
│   │   ├── DashboardTemplate.vue      # 仪表盘模板
│   │   └── DynamicMenu.vue            # 动态菜单
│   ├── chart/
│   │   ├── EchartsLine.vue            # 折线图组件
│   │   ├── EchartsBar.vue             # 柱状图组件
│   │   ├── EchartsPie.vue             # 饼图组件
│   │   └── Echarts3D.vue              # 3D图表组件
│   └── three/
│       └── ThreeDViewer.vue            # Three.js 3D查看器
├── views/
│   └── dashboard/
│       └── DashboardView.vue           # 仪表盘页面
├── App.vue
└── main.ts
```

### 2.3 核心模块说明

#### 2.3.1 认证模块（JWT + RBAC）

**认证流程：**
```
1. 用户 POST /api/auth/login {username, password}
2. JwtAuthFilter 拦截，验证用户名密码
3. JwtUtil 生成 JWT Token（包含userId, roles, exp）
4. 返回 {token, refreshToken, userId}
5. 前端存储 token，每次请求携带
6. JwtAuthFilter 验证每个请求的Token有效性
7. 从Token中解析用户权限，存入SecurityContext
```

**权限控制流程：**
```
1. 用户登录 → 获取角色列表 → 获取菜单权限
2. 后端动态菜单数据返回前端
3. 前端根据菜单数据动态渲染侧边栏
4. 路由守卫检查Token有效性
5. 按钮级别权限：后端返回按钮权限标识，前端v-if控制显隐
```

#### 2.3.2 文件存储模块（MinIO + 本地降级）

**存储策略模式：**
```java
// MinioUtil 核心判断逻辑
if (storageProvider == "minio" && minioAvailable) {
    // 使用MinIO分布式存储
    uploadToMinio();
} else {
    // 降级到本地文件系统
    uploadLocally();
}
```

**上传流程：**
```
1. 前端 POST /api/file/upload (multipart/form-data)
2. FileController 接收文件
3. 文件类型白名单校验（jpg/png/pdf/docx等）
4. 文件大小校验（单文件≤10MB）
5. MinioUtil.uploadFile() 判断存储模式
6. 生成UUID文件名（防覆盖）
7. 存入MinIO或本地目录
8. 记录操作日志
9. 返回文件访问URL
10. 触发TTS语音播报（异步）
```

#### 2.3.3 TTS语音模块（策略模式）

**接口抽象：**
```java
public interface TtsService {
    String synthesize(String text, String voiceId, ...);
    boolean isMockMode();
    String getProviderName();
}
```

**实现类：**
| 实现类 | 触发条件 | 效果 |
|--------|----------|------|
| MinimaxTtsUtil | minimax.api-key配置了有效值 | 调用Minimax T2A V2 API |
| MockTtsServiceImpl | 未配置api-key或provider=mock | 彩色日志输出 |

#### 2.3.4 AOP日志模块

**切面配置：**
```java
@Aspect
@Component
@Slf4j
public class OperationLogAspect {
    // 拦截所有 @OperationLog 注解的方法
    // 记录内容：用户名、操作描述、方法名、IP、耗时、返回结果
}
```

---

## 第三章 数据库设计

### 3.1 E-R图（文字描述版）

```
                         ┌─────────────────┐
                         │    SysMenu      │
                         │   (菜单资源)     │
                         └────────┬────────┘
                                  │
                                  │ N:M（角色菜单关联）
                                  ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    SysUser      │ N:1│  SysUserRole     │ N:1│    SysRole      │
│   (系统用户)     │◄───│  (用户角色关联)   │───►│   (系统角色)     │
└────────┬────────┘    └─────────────────┘    └─────────────────┘
         │
         │ 1:N（操作日志归属）
         ▼
┌─────────────────┐
│SysOperationLog  │
│   (操作日志)     │
└─────────────────┘
```

### 3.2 数据表总览

| 序号 | 表名 | 说明 | 主键 |
|------|------|------|------|
| 1 | sys_user | 系统用户表 | id |
| 2 | sys_role | 系统角色表 | id |
| 3 | sys_user_role | 用户角色关联表 | user_id, role_id |
| 4 | sys_menu | 菜单资源表 | id |
| 5 | sys_role_menu | 角色菜单关联表 | role_id, menu_id |
| 6 | sys_operation_log | 操作日志表 | id |
| 7 | template_entity | 通用实体母版 | id |

### 3.3 表结构详细说明

详见《数据库设计文档》- `database_design.md`

---

## 第四章 接口设计

### 4.1 接口设计原则

1. **RESTful风格**：使用HTTP动词表达操作（GET/POST/PUT/DELETE）
2. **统一响应格式**：所有接口返回 `Result<T>` 结构
3. **分页规范**：列表接口统一使用 `PageParam` 分页参数
4. **认证规范**：除登录接口外，所有接口需要JWT Token

### 4.2 主要接口一览

#### 认证模块

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/auth/login` | POST | username, password | 用户登录 |
| `/api/auth/logout` | POST | - | 登出 |
| `/api/auth/refresh` | POST | refreshToken | 刷新Token |

#### 用户管理

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/user/page` | GET | current, size, keyword | 分页查询 |
| `/api/user/{id}` | GET | - | 详情 |
| `/api/user` | POST | User实体 | 新增 |
| `/api/user` | PUT | User实体 | 修改 |
| `/api/user/{id}` | DELETE | - | 删除 |
| `/api/user/{id}/roles` | PUT | roleIds | 分配角色 |

#### 角色管理

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/role/page` | GET | current, size | 分页查询 |
| `/api/role` | POST | Role实体 | 新增 |
| `/api/role` | PUT | Role实体 | 修改 |
| `/api/role/{id}` | DELETE | - | 删除 |
| `/api/role/{id}/menus` | PUT | menuIds | 分配菜单 |

#### 文件管理

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/file/upload` | POST | file | 单文件上传 |
| `/api/file/upload/batch` | POST | files | 批量上传 |
| `/api/file` | DELETE | url | 删除文件 |
| `/api/file/batch` | DELETE | urls | 批量删除 |

#### TTS语音

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/tts/broadcast` | POST | text, voiceId | 语音播报 |
| `/api/tts/status` | GET | - | TTS状态 |

---

## 第五章 安全设计

### 5.1 认证安全

- **JWT无状态认证**：Token携带用户身份信息，服务器无需存储Session
- **Token有效期**：AccessToken 2小时，RefreshToken 7天
- **密码加密**：BCrypt单向加密，即使数据库泄露也无法还原明文

### 5.2 接口安全

- **参数校验**：@Valid注解+JSR-303校验
- **SQL注入**：MyBatis-Plus参数化查询
- **XSS**：前端输入转义
- **CORS**：仅允许配置的域名跨域

### 5.3 数据安全

- **逻辑删除**：所有删除操作仅修改is_deleted标志，数据可恢复
- **操作日志**：敏感操作全部记录日志
- **敏感信息**：日志中不输出密码、Token等敏感字段

---

## 第六章 部署架构

### 6.1 开发环境

```
前端：localhost:5173（Vite开发服务器）
后端：localhost:8080（Spring Boot）
MySQL：localhost:3306
MinIO：localhost:9000（API），localhost:9001（Console）
文档：localhost:8080/doc.html（Knife4j）
```

### 6.2 生产环境

```
Nginx（80/443）
  ├── /api/*     →  反向代理 → Spring Boot :8080
  ├── /local-files/* → 静态文件目录
  └── /*         →  Vue3构建产物

Spring Boot（:8080）
  ├── MySQL（远程或容器）
  ├── MinIO（远程或容器）
  └── Redis（可选，缓存）

MinIO（:9000/9001）
  └── generic-sys-admin桶（公开读）
```

---

*文档版本：v1.0*
*最后更新：2026-04-19*
