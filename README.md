# Generic Sys Admin - 通用企业资源管理系统 (ERMS)

> 企业级设备、耗材、用户及权限管理平台，采用前后端分离架构，覆盖设备全生命周期管理、耗材库存管理、角色权限控制与操作审计。

## 项目简介

**Generic Sys Admin**（通用企业资源管理系统）是一款面向企业级用户的资源管理软件，旨在帮助组织实现设备、耗材等实物资产的数字化管理。系统提供设备台账管理、耗材库存管理、用户身份认证、角色权限控制、操作日志审计等核心功能，支持企业建立规范化的资产管理体系。

本项目采用业界成熟的 **Spring Boot + MyBatis-Plus + Vue 3** 技术栈，后端提供标准 RESTful API，前端基于 Element Plus 构建，支持本地化部署或容器化部署。

### 核心特性

| 特性 | 说明 |
|------|------|
| **RBAC 权限模型** | 基于角色的访问控制，支持多级角色和资源权限精细化管理 |
| **NL 业务流编排** | 基于规则引擎的自然语言业务流解析，降低操作门槛 |
| **实时状态监控** | WebSocket 推送设备/耗材状态变更，监控面板实时更新 |
| **AI 服务集成** | 内置 MiniMax 多模态 AI 服务（TTS/图像生成/视频生成） |
| **全链路审计** | AOP 切面自动记录所有关键操作，支持完整追溯链 |
| **Spring Boot 3.x** | 采用最新 Spring Boot 3.4 + Spring Security 6.x |

---

## 目录

- [项目背景](#项目背景)
- [核心价值](#核心价值)
- [技术架构](#技术架构)
- [功能模块](#功能模块)
- [NL自然语言业务流](#nl自然语言业务流)
- [RBAC权限模型](#rbac权限模型)
- [数据库设计](#数据库设计)
- [API文档](#api文档)
- [部署指南](#部署指南)
- [配置参考](#配置参考)

---

## 项目背景

### 企业资源管理数字化的行业需求

随着企业规模的扩大和信息化程度的提升，实物资产（设备、工具、耗材）的管理面临以下挑战：

1. **资产台账不清**：设备分布分散，缺乏统一的编号和台账系统，导致资产盘点困难、家底不清。
2. **耗材浪费严重**：耗材领用缺乏记录和预警机制，重复采购、库存积压或临时短缺并存。
3. **维护记录缺失**：设备维护历史分散在纸质文档或电子表格中，无法追踪设备健康状态。
4. **权限控制薄弱**：系统缺乏精细的权限管理，普通用户可执行高危操作，安全风险高。
5. **审计追溯困难**：操作行为无完整日志，出现问题时难以追溯原因和责任人。

### 解决方案

本系统针对上述痛点，提供以下核心能力：

- 统一的设备台账与唯一编号体系，扫码即可查看设备档案
- 耗材库存实时管理，支持入库、出库、预警和盘点全流程
- 基于 RBAC 的角色权限体系，精确控制用户可访问的功能和数据
- 完整的操作审计日志，所有关键操作均可追溯
- 自然语言业务流编排，非技术人员可通过自然语言指令操作系统

---

## 核心价值

### 学术价值

| 方面 | 说明 |
|------|------|
| **工程实践** | 完整实现了一套 RBAC（Role-Based Access Control）权限模型，包含用户-角色-权限三层关联，与标准 NIST RBAC 模型一致 |
| **架构设计** | 采用前后端分离架构，后端 Spring Boot 提供 RESTful API，前端 Vue 3 实现 SPA 应用，体现现代 Web 开发最佳实践 |
| **数据库设计** | 遵循范式设计，包含逻辑删除、索引优化、外键约束等企业级数据库设计要素 |
| **安全机制** | 集成 BCrypt 密码加密、JWT 无状态认证、账户锁定机制，提升系统安全性 |
| **规则引擎** | 实现基于规则的自然语言业务流解析引擎，支持NL指令到系统操作的转换 |

### 商业价值

| 方面 | 说明 |
|------|------|
| **快速交付** | 基于成熟开源技术栈，学习曲线低，可快速部署落地 |
| **灵活扩展** | 模块化设计，支持功能扩展（如工单管理、采购管理） |
| **降低风险** | 操作日志全覆盖，支持合规审计需求 |
| **资产可见** | 实时掌握设备状态和耗材库存，优化资产利用率 |
| **操作简化** | NL业务流编排降低使用门槛，普通员工可直接操作 |

---

## 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Layer                                 │
│   Vue 3 + TypeScript + Element Plus + Axios + Pinia + WebSocket     │
└────────────────────────────────────┬────────────────────────────────┘
                                     │ HTTP/REST (JSON) + WebSocket
┌────────────────────────────────────▼────────────────────────────────┐
│                    API Layer (Spring Boot 3.4)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │Equipment │ │Consumable│ │   User   │ │   Role   │  ...           │
│  │Controller│ │Controller│ │Controller│ │Controller│               │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘               │
│       │            │            │            │                      │
│  ┌────▼────────────▼────────────▼────────────▼────┐                │
│  │              Service Layer                      │                │
│  │  EquipmentService  ConsumableService  ...       │                │
│  └────┬────────────┬────────────┬─────────────────┘                │
│       │            │            │                                  │
│  ┌────▼────────────▼────────────▼─────────────────┐                │
│  │           MyBatis-Plus ORM                     │                │
│  └──────────────────┬─────────────────────────────┘                │
└─────────────────────┼──────────────────────────────────────────────┘
                      │ JDBC
┌─────────────────────▼─────────────────────────────────────────────┐
│                        Data Layer                                    │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐ │
│  │     MySQL 8.0   │    │  Redis (可选)    │    │   WebSocket     │ │
│  │   主数据存储     │    │    缓存/会话     │    │   实时推送      │ │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### 后端技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **运行环境** | Java | 17+ | LTS 版本，长期支持 |
| **核心框架** | Spring Boot | 3.4.x | 生态丰富，开箱即用 |
| **安全框架** | Spring Security | 6.x | 认证与授权 |
| **认证令牌** | JWT (jjwt) | 0.12.x | 无状态身份认证 |
| **ORM** | MyBatis-Plus | 3.5.x | 增强 MyBatis，简化 CRUD |
| **数据库** | MySQL | 8.0+ | 主数据存储 |
| **缓存** | Redis | 7.x | 会话缓存（可选，无 Redis 自动降级） |
| **WebSocket** | Spring WebSocket | 6.x | 实时状态推送 |
| **规则引擎** | 自研 NL 解析器 | 1.0 | 自然语言业务流解析 |
| **API 文档** | Knife4j / Swagger | 4.x | 在线 API 文档与测试 |
| **工具库** | Hutool / Lombok | 5.x / 1.18.x | 简化开发 |
| **构建工具** | Maven | 3.8+ | 依赖管理与构建 |

### 前端技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **核心框架** | Vue | 3.x | 渐进式前端框架 |
| **类型系统** | TypeScript | 5.x | 类型安全 |
| **UI 组件库** | Element Plus | 2.x | 企业级 Vue 3 组件库 |
| **构建工具** | Vite | 5.x | 快速构建工具 |
| **状态管理** | Pinia | 2.x | Vue 3 专用状态管理 |
| **HTTP 客户端** | Axios | 1.x | HTTP 请求封装 |
| **路由管理** | Vue Router | 4.x | SPA 路由 |

### 项目目录结构

```
generic-sys-admin/
├── backend/                          # 后端 Spring Boot 项目
│   ├── src/main/java/com/zyy/
│   │   ├── controller/               # REST 控制器
│   │   │   ├── SysUserController.java      # 用户管理
│   │   │   ├── RoleController.java         # 角色管理
│   │   │   ├── MenuController.java         # 菜单管理
│   │   │   ├── EquipmentController.java    # 设备管理
│   │   │   ├── ConsumableController.java   # 耗材管理
│   │   │   ├── InventoryRecordController.java  # 设备巡检记录
│   │   │   ├── OperationLogController.java # 操作日志
│   │   │   ├── AIController.java           # AI 服务（TTS/图像/视频）
│   │   │   ├── DashboardController.java    # 监控面板
│   │   │   └── FileController.java         # 文件上传
│   │   ├── service/                  # 业务逻辑层
│   │   │   ├── impl/
│   │   │   ├── NLService.java        # 自然语言业务流解析
│   │   │   └── WebSocketService.java # WebSocket实时推送
│   │   ├── mapper/                   # 数据访问层
│   │   ├── model/
│   │   │   ├── entity/               # 数据库实体（与表一一对应）
│   │   │   ├── dto/                  # 数据传输对象（请求）
│   │   │   └── vo/                   # 视图对象（响应）
│   │   ├── rbac/                     # RBAC 权限核心模块
│   │   │   ├── model/
│   │   │   ├── service/
│   │   │   └── filter/
│   │   ├── security/                 # 安全认证模块
│   │   │   ├── JwtAuthFilter.java   # JWT 认证过滤器
│   │   │   └── SecurityConfig.java  # 安全配置
│   │   ├── config/                   # 配置类
│   │   ├── aspect/                   # 切面（日志记录）
│   │   ├── websocket/                # WebSocket 配置
│   │   ├── nl/                       # 自然语言解析引擎
│   │   │   ├── parser/              # 解析器
│   │   │   ├── rules/               # 规则定义
│   │   │   └── executor/            # 执行器
│   │   ├── exception/                # 全局异常处理
│   │   ├── enums/                    # 枚举类型
│   │   └── util/                     # 工具类
│   └── src/main/resources/
│       ├── application.yml           # 主配置文件
│       ├── application-dev.yml        # 开发环境配置
│       └── nl-rules/                  # NL业务流规则配置
├── frontend/                         # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                      # API 请求封装
│   │   ├── views/                   # 页面组件
│   │   │   ├── dashboard/            # 监控面板
│   │   │   ├── system/               # 系统管理（用户/角色/菜单/日志）
│   │   │   ├── equipment/
│   │   │   ├── consumable/
│   │   │   ├── nl/                   # 自然语言操作界面
│   │   │   └── inventory/
│   │   ├── router/                  # 路由配置
│   │   ├── stores/                  # Pinia 状态管理
│   │   ├── utils/                   # 工具函数
│   │   ├── websocket/               # WebSocket 客户端
│   │   └── App.vue
│   └── package.json
├── sql/                              # 数据库脚本
│   ├── v1.0__init.sql               # 完整初始化脚本（含示例数据）
│   └── v1.1__operation_log.sql      # 操作日志表（增量）
├── docker-compose.yml               # Docker 容器编排
├── start.sh                         # 一键启动脚本
└── pom.xml                          # Maven 配置（后端依赖）
```

---

## 功能模块

### 1. 用户管理（SysUser）

管理系统用户账户，支持用户的增删改查、状态管理、密码重置等功能。

| 功能 | 说明 |
|------|------|
| 用户列表 | 分页展示用户，支持按用户名/手机号/状态筛选 |
| 新增用户 | 创建用户账号，初始密码可自定义 |
| 编辑用户 | 修改用户基本信息（姓名、邮箱、手机号等） |
| 删除用户 | 逻辑删除，而非物理删除，保护数据完整性 |
| 密码重置 | 管理员可强制重置用户密码 |
| 角色分配 | 为用户分配一个或多个系统角色 |
| 账户锁定 | 连续登录失败自动锁定账户，防暴力破解 |

### 2. 角色权限管理（SysRole + SysMenu）

基于 RBAC（Role-Based Access Control）模型的权限管理系统，是系统的安全核心。

| 概念 | 说明 |
|------|------|
| **用户（User）** | 系统的实际操作者 |
| **角色（Role）** | 一组权限的集合，如"系统管理员"、"操作员"、"查看者" |
| **权限（Permission）** | 通过菜单（Menu）体现，前端路由受 RBAC 控制 |
| **用户-角色关联** | 多对多，一个用户可拥有多个角色 |
| **角色-菜单关联** | 多对多，一个角色可访问多个菜单 |

**权限控制粒度**：

- 后端：Spring Security + Method Security，接口级权限校验
- 前端：路由守卫 + 动态菜单，仅展示用户有权限访问的菜单项

### 3. 设备管理（SysEquipment）

管理企业固定资产——设备，支持设备全生命周期追踪。

| 功能 | 说明 |
|------|------|
| 设备台账 | 设备编号、名称、分类、型号、生产商、出厂日期等 |
| 状态流转 | 设备状态：正常 / 维护中 / 报废 |
| 位置管理 | 记录设备当前存放或使用地点 |
| 维保管理 | 维保周期设置，下次维保日期自动计算 |
| 巡检记录 | 关联 `sys_inventory_record` 表，记录每次巡检维保详情 |
| WebSocket 推送 | 设备状态变更实时推送到监控面板 |

**设备状态枚举**：

```
0 = 维护中 (Under Maintenance)
1 = 正常 (Normal)
2 = 报废 (Scrapped)
```

### 4. 耗材管理（SysConsumable）

管理企业消耗性物资（耗材）的库存，支持入库、出库、预警全流程。

| 功能 | 说明 |
|------|------|
| 耗材台账 | 产品编号、名称、分类、单位、单价、供应商 |
| 库存数量 | 实时库存数量，最小/最大库存阈值设置 |
| 库存预警 | 库存低于最小阈值时提醒补货 |
| 入库管理 | 采购入库，记录数量、参考单号、操作人 |
| 出库管理 | 领用出库，记录消耗用途、参考单号 |
| 批次管理 | 支持设置有效期（Expiration Date），可按批次出库 |
| 盘点 | 支持定期盘点，更新实际库存数量 |

**交易类型（Transaction Type）**：

```
INBOUND   = 入库（采购/退货入库）
OUTBOUND  = 出库（领用/损耗）
ADJUSTMENT = 盘点调整
RETURN    = 退货
```

### 5. 操作日志（SysOperationLog）

AOP 切面自动记录所有关键操作，形成完整的审计追踪链。

| 记录字段 | 说明 |
|------|------|
| 用户 | 操作人用户名（反规范化，查询更高效） |
| 模块 | 业务模块（如 Equipment、Consumable） |
| 操作类型 | SELECT / INSERT / UPDATE / DELETE |
| 目标表/ID | 被操作的表名和主键 |
| 请求参数 | 完整请求参数（JSON 格式） |
| 请求方法/URL | HTTP 方法和请求路径 |
| IP 地址 | 客户端 IP，追溯来源 |
| 执行时长 | 接口响应时间（毫秒），发现性能问题 |
| 结果状态 | 成功 / 失败 |
| 错误详情 | 失败时的异常堆栈信息 |

### 6. AI 服务模块（AIController）

集成 MiniMax 多模态 AI 能力，提供以下在线服务（需配置 API Key）：

| 服务 | 模型 | 说明 |
|------|------|------|
| 语音合成（TTS） | speech-01 | 文本转语音，支持多种音色选择 |
| 图像生成 | image-01 | 文本描述生成图像 |
| 视频生成 | video-01 | 文本描述生成视频（异步任务） |

> 注意：AI 模块为独立功能，与核心 ERMS 业务逻辑无关，仅提供辅助能力。

### 7. 监控面板（DashboardController）

通过 WebSocket 实时推送设备/耗材状态变更。

```java
// 监控数据类型
- 设备状态变更（正常→维护中→报废）
- 耗材库存预警（低于最小阈值）
- 新增操作日志
- 用户登录事件
```

前端通过 WebSocket 订阅主题，实时接收推送数据并更新监控面板。

---

## NL自然语言业务流

### 概述

NL（Natural Language）自然语言业务流是本系统的核心创新之一。它允许用户通过自然语言指令操作系统，降低使用门槛，无需深入学习即可完成复杂业务操作。

### 工作原理

```
用户输入："查询设备编号为 EQ-2024-001 的维护记录"
     │
     ▼
┌─────────────────────────────────────┐
│        NL Parser（解析器）           │
│  1. 分词与词性标注                    │
│  2. 实体识别（设备编号、日期等）      │
│  3. 意图分类（查询/创建/更新/删除）   │
│  4. 槽位填充                        │
└─────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│       Rule Engine（规则引擎）        │
│  匹配业务规则模板，生成执行计划        │
└─────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│      Executor（执行器）              │
│  调用对应 Service 完成业务操作        │
└─────────────────────────────────────┘
     │
     ▼
返回执行结果给用户
```

### 支持的NL指令类型

| 指令示例 | 意图 | 参数 |
|----------|------|------|
| "查询设备 EQ-2024-001" | 查询设备 | deviceCode=EQ-2024-001 |
| "列出所有维护中的设备" | 列表查询 | status=0 |
| "给设备 EQ-2024-001 做巡检" | 创建记录 | deviceCode, inspectionType |
| "入库耗材键盘 50个" | 耗材入库 | productName, quantity |
| "查询我的操作日志" | 日志查询 | currentUser |
| "给用户张三分配管理员角色" | 用户授权 | username, roleName |

### NL解析器核心实现

```java
/**
 * NL指令解析服务
 */
@Service
public class NLService {
    
    @Autowired
    private NLParser parser;
    
    @Autowired
    private NLRuleEngine ruleEngine;
    
    @Autowired
    private NLExecutor executor;
    
    /**
     * 解析并执行自然语言指令
     */
    public NLResult execute(String nlCommand) {
        // 1. 解析NL指令
        NLContext context = parser.parse(nlCommand);
        
        // 2. 匹配业务规则
        NLExtraction extraction = ruleEngine.match(context);
        
        // 3. 校验参数
        if (!extraction.isValid()) {
            return NLResult.fail("参数缺失: " + extraction.getMissingParams());
        }
        
        // 4. 执行操作
        return executor.execute(extraction);
    }
}
```

### NL规则配置示例

```yaml
# nl-rules/device-query.yaml
intent: QUERY_DEVICE
patterns:
  - "查询设备 {deviceCode}"
  - "查看设备 {deviceCode}"
  - "设备 {deviceCode} 的信息"
entities:
  deviceCode:
    type: STRING
    required: true
    pattern: "^EQ-\\d{4}-\\d{3}$"
response:
  format: "table"
  fields: [code, name, category, status, location]
```

---

## RBAC权限模型

### 模型架构

本系统采用基于 NIST RBAC 标准的四层权限模型：

```
┌─────────────────────────────────────────────────────────────┐
│                      Permission（权限）                      │
│              通过 Menu（菜单）体现，支持 CRUD 操作              │
└─────────────────────────────────────────────────────────────┘
                              ▲ N:M
                              │
┌─────────────────────────────────────────────────────────────┐
│                      Role（角色）                             │
│        ROLE_ADMIN / ROLE_OPERATOR / ROLE_VIEWER 等           │
└─────────────────────────────────────────────────────────────┘
                              ▲ N:M
                              │
┌─────────────────────────────────────────────────────────────┐
│                      User（用户）                            │
│                   系统实际操作者                              │
└─────────────────────────────────────────────────────────────┘
```

### 角色定义

| 角色代码 | 角色名称 | 权限描述 |
|----------|----------|----------|
| ROLE_ADMIN | 系统管理员 | 全部功能，包含用户管理、角色管理、系统配置 |
| ROLE_EQUIPMENT_ADMIN | 设备管理员 | 设备全生命周期管理、巡检记录 |
| ROLE_CONSUMABLE_ADMIN | 耗材管理员 | 耗材库存管理、入库出库操作 |
| ROLE_OPERATOR | 操作员 | 设备巡检、耗材领用 |
| ROLE_VIEWER | 查看者 | 仅查看，无写操作权限 |

### 权限控制实现

#### 后端接口权限控制

```java
@PreAuthorize("hasRole('ROLE_ADMIN') or hasAuthority('equipment:write')")
@PostMapping("/equipment")
public Result<EquipmentVO> createEquipment(@RequestBody EquipmentDTO dto) {
    // 只有 ROLE_ADMIN 或有 equipment:write 权限的用户才能访问
    return Result.ok(equipmentService.create(dto));
}

@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
@GetMapping("/equipment")
public Result<PageResult<EquipmentVO>> listEquipment(EquipmentQuery query) {
    return Result.ok(equipmentService.list(query));
}
```

#### 前端路由守卫

```typescript
// 路由配置
const routes = [
  {
    path: '/system/users',
    component: UserManagement,
    meta: { roles: ['ROLE_ADMIN'] }  // 只有管理员可访问
  },
  {
    path: '/equipment',
    component: EquipmentList,
    meta: { roles: ['ROLE_ADMIN', 'ROLE_EQUIPMENT_ADMIN', 'ROLE_OPERATOR'] }
  }
]

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiredRoles = to.meta.roles || []
  
  if (requiredRoles.length === 0 || 
      requiredRoles.some(role => userStore.roles.includes(role))) {
    next()
  } else {
    next('/403')  // 无权限跳转403
  }
})
```

---

## 数据库设计

### ER 图（实体关系）

```
                    ┌──────────────┐
                    │   sys_user   │
                    │──────────────│
                    │ PK id        │
                    │ uk username  │
                    │ password     │
                    │ status       │
                    └──────┬───────┘
                           │ N:M
                    ┌──────▼───────┐
                    │ sys_user_role│
                    │──────────────│
                    │ PK id        │
                    │ FK user_id   │
                    │ FK role_id   │
                    └──────┬───────┘
                           │ N:1
              ┌────────────▼────────────┐
              │       sys_role           │
              │─────────────────────────│
              │ PK id                   │
              │ uk code                 │
              │ name                    │
              │ description             │
              └────────────┬────────────┘
                           │ N:M
              ┌────────────▼────────────┐
              │     sys_role_menu       │
              │─────────────────────────│
              │ PK id                   │
              │ FK role_id              │
              │ FK menu_id              │
              └────────────┬────────────┘
                           │ N:1
              ┌────────────▼────────────┐
              │       sys_menu          │
              │─────────────────────────│
              │ PK id                   │
              │ uk path                 │
              │ FK parent_id (自引用)    │
              │ name / icon / sort      │
              └─────────────────────────┘


    ┌──────────────┐     ┌──────────────────────┐
    │sys_equipment │     │ sys_consumable       │
    │──────────────│     │──────────────────────│
    │ PK id        │     │ PK id                │
    │ uk equip_code│     │ uk product_code      │
    │ name/category│     │ name/category        │
    │ status       │     │ stock_quantity       │
    └──────┬───────┘     │ min_stock_level      │
           │ 1:N          └──────────┬───────────┘
    ┌──────▼────────────┐   ┌───────▼───────────┐
    │sys_inventory_record│   │sys_inventory_trans│
    │───────────────────│   │───────────────────│
    │ PK id             │   │ PK id             │
    │ FK equipment_id   │   │ FK consumable_id  │
    │ inspection_type   │   │ transaction_type  │
    │ inspection_date   │   │ quantity          │
    │ result            │   │ balance_after     │
    └───────────────────┘   │ operator_id       │
                            └───────────────────┘

    ┌────────────────────────┐
    │  sys_operation_log      │
    │────────────────────────│
    │ PK id                  │
    │ FK user_id (可空)      │
    │ module / operation     │
    │ target_table / target_id│
    │ request_params         │
    │ ip_address             │
    │ operation_time         │
    │ duration_ms            │
    │ result_status          │
    └────────────────────────┘

    ┌────────────────────────┐
    │  sys_nl_command_log     │
    │────────────────────────│
    │ PK id                  │
    │ FK user_id             │
    │ nl_command             │
    │ intent                 │
    │ extraction_params      │
    │ execution_result       │
    │ execution_time_ms      │
    └────────────────────────┘
```

### 主要数据表说明

#### sys_user（系统用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(64) | 用户名，全局唯一 |
| password | VARCHAR(255) | BCrypt 加密后的密码 |
| real_name | VARCHAR(128) | 真实姓名 |
| email | VARCHAR(128) | 邮箱 |
| phone | VARCHAR(32) | 手机号 |
| status | TINYINT | 状态：0=禁用, 1=正常, 2=锁定 |
| last_login_ip | VARCHAR(64) | 最后登录 IP |
| last_login_at | DATETIME | 最后登录时间 |
| failed_attempts | INT | 连续登录失败次数 |
| locked_until | DATETIME | 账户锁定截止时间 |
| is_deleted | TINYINT | 逻辑删除标记 |

#### sys_role（系统角色表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(64) | 角色名称（如"系统管理员"） |
| code | VARCHAR(64) | 角色代码（如"ROLE_ADMIN"），唯一 |
| description | VARCHAR(255) | 角色描述 |
| status | TINYINT | 状态：0=禁用, 1=正常 |
| sort_order | INT | 排序序号 |

#### sys_menu（系统菜单表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(64) | 菜单名称 |
| path | VARCHAR(128) | 路由路径，唯一 |
| component | VARCHAR(255) | Vue 组件路径 |
| icon | VARCHAR(64) | 图标 |
| parent_id | BIGINT | 父菜单 ID（自引用） |
| sort_order | INT | 排序序号 |
| type | TINYINT | 类型：1=目录, 2=菜单, 3=按钮 |
| permission | VARCHAR(64) | 权限标识（如 equipment:write） |

#### sys_equipment（设备表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| equipment_code | VARCHAR(64) | 设备编号，全局唯一 |
| name | VARCHAR(128) | 设备名称 |
| category | VARCHAR(64) | 设备分类 |
| model | VARCHAR(128) | 设备型号 |
| manufacturer | VARCHAR(128) | 生产厂商 |
| purchase_date | DATE | 采购日期 |
| warranty_expiry | DATE | 保修截止日期 |
| status | TINYINT | 状态：0=维护, 1=正常, 2=报废 |
| location | VARCHAR(256) | 存放位置 |
| maintenance_cycle_days | INT | 维保周期（天） |
| next_maintenance_date | DATE | 下次维保日期 |

#### sys_consumable（耗材表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| product_code | VARCHAR(64) | 产品编号，全局唯一 |
| name | VARCHAR(128) | 产品名称 |
| category | VARCHAR(64) | 产品分类 |
| unit | VARCHAR(32) | 计量单位（默认 piece） |
| stock_quantity | INT | 当前库存数量 |
| min_stock_level | INT | 最小库存预警阈值 |
| max_stock_level | INT | 最大库存上限 |
| unit_cost | DECIMAL(10,2) | 单价 |
| expiration_date | DATE | 有效期（可选） |
| supplier | VARCHAR(128) | 供应商 |
| reorder_point | INT | 补货触发点 |

#### sys_inventory_transaction（库存流水表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| consumable_id | BIGINT | 关联耗材 ID |
| transaction_type | VARCHAR(32) | 交易类型 |
| quantity | INT | 变动数量（正数入库，负数出库） |
| balance_after | INT | 变动后余额 |
| reference_no | VARCHAR(64) | 参考单号（如采购单号） |
| operator_id | BIGINT | 操作人 |
| transaction_time | DATETIME | 交易时间 |

#### sys_nl_command_log（NL指令日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 操作人 ID |
| nl_command | VARCHAR(512) | 原始 NL 指令 |
| intent | VARCHAR(64) | 识别的意图 |
| extraction_params | TEXT | 解析后的参数（JSON） |
| execution_result | TEXT | 执行结果（JSON） |
| execution_time_ms | INT | 执行时长 |
| operation_time | DATETIME | 操作时间 |

---

## API文档

### 认证接口

#### POST /api/auth/login

用户登录，获取 JWT 令牌。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbG...9...",
    "userId": 1,
    "username": "admin",
    "realName": "System Administrator",
    "roles": ["ROLE_ADMIN"]
  }
}
```

### 用户管理接口

#### GET /api/users

获取用户列表（分页）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 10 |
| username | String | 否 | 按用户名模糊搜索 |
| status | int | 否 | 按状态筛选 |

#### POST /api/users

新增用户。

#### PUT /api/users/{id}

更新用户信息。

#### DELETE /api/users/{id}

删除用户（逻辑删除）。

#### PUT /api/users/{id}/reset-password

管理员重置用户密码。

#### PUT /api/users/{id}/roles

为用户分配角色。

### 角色管理接口

#### GET /api/roles

获取角色列表。

#### POST /api/roles

新增角色。

#### PUT /api/roles/{id}

更新角色。

#### DELETE /api/roles/{id}

删除角色。

#### GET /api/roles/{id}/menus

获取角色已分配的菜单 ID 列表。

#### PUT /api/roles/{id}/menus

为角色分配菜单权限。

### 设备管理接口

#### GET /api/equipment

获取设备列表（分页）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |
| equipmentCode | String | 否 | 设备编号（精确匹配） |
| name | String | 否 | 设备名称（模糊匹配） |
| category | String | 否 | 设备分类 |
| status | int | 否 | 设备状态 |

#### POST /api/equipment

新增设备。

#### PUT /api/equipment/{id}

更新设备信息。

#### DELETE /api/equipment/{id}

删除设备（逻辑删除）。

#### GET /api/equipment/{id}/records

获取设备的巡检维保记录。

#### POST /api/equipment/{id}/inspect

创建设备巡检记录。

### 耗材管理接口

#### GET /api/consumables

获取耗材列表（分页）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |
| productCode | String | 否 | 产品编号 |
| name | String | 否 | 产品名称 |
| category | String | 否 | 产品分类 |

#### POST /api/consumables

新增耗材。

#### PUT /api/consumables/{id}

更新耗材信息。

#### DELETE /api/consumables/{id}

删除耗材。

#### POST /api/consumables/{id}/inbound

耗材入库。

**请求体**：

```json
{
  "quantity": 50,
  "referenceNo": "PO-2024-001",
  "remarks": "采购入库"
}
```

#### POST /api/consumables/{id}/outbound

耗材出库。

**请求体**：

```json
{
  "quantity": 5,
  "referenceNo": "REQ-2024-001",
  "remarks": "领用"
}
```

### NL自然语言接口

#### POST /api/nl/execute

执行自然语言指令。

**请求体**：

```json
{
  "command": "查询设备编号为 EQ-2024-001 的维护记录"
}
```

**响应示例**：

```json
{
  "code": 200,
  "msg": "执行成功",
  "data": {
    "intent": "QUERY_EQUIPMENT_RECORD",
    "params": {
      "deviceCode": "EQ-2024-001"
    },
    "result": {
      "records": [...]
    },
    "executionTimeMs": 45
  }
}
```

#### GET /api/nl/history

获取 NL 指令执行历史。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

### WebSocket 接口

#### 连接地址

```
ws://localhost:8080/ws/monitor
```

#### 订阅主题

| 主题 | 说明 | 消息格式 |
|------|------|----------|
| /topic/equipment/status | 设备状态变更 | `{type: "EQUIPMENT_STATUS_CHANGE", data: {...}}` |
| /topic/consumable/warning | 耗材库存预警 | `{type: "CONSUMABLE_LOW_STOCK", data: {...}}` |
| /topic/operation/log | 新操作日志 | `{type: "NEW_OPERATION_LOG", data: {...}}` |

#### 前端订阅示例

```javascript
const socket = new SockJS('/ws/monitor')
const stompClient = Stomp.over(socket)

stompClient.connect({}, function(frame) {
    // 订阅设备状态变更
    stompClient.subscribe('/topic/equipment/status', function(message) {
        const data = JSON.parse(message.body)
        console.log('设备状态变更:', data)
        // 更新监控面板
    })
    
    // 订阅耗材预警
    stompClient.subscribe('/topic/consumable/warning', function(message) {
        const data = JSON.parse(message.body)
        console.log('耗材预警:', data)
        // 显示预警通知
    })
})
```

### 日志接口

#### GET /api/logs

获取操作日志列表（分页）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |
| username | String | 否 | 操作人 |
| module | String | 否 | 业务模块 |
| operation | String | 否 | 操作类型 |
| startTime | String | 否 | 开始时间（yyyy-MM-dd） |
| endTime | String | 否 | 结束时间 |

### AI 服务接口

#### POST /api/ai/tts

语音合成。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| text | String | 是 | 合成文本 |
| voice | String | 否 | 音色，默认 male-qn-qingse |

#### POST /api/ai/image

图像生成。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prompt | String | 是 | 图像描述 |
| aspectRatio | String | 否 | 尺寸比例：1:1/16:9/9:16/3:4/4:3 |

#### POST /api/ai/video/generate

视频生成（异步）。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prompt | String | 是 | 视频描述 |
| duration | Integer | 否 | 时长：5/10 秒 |

#### GET /api/ai/video/status/{jobId}

查询视频生成状态。

### 通用响应格式

所有 API 采用统一的响应包装格式：

```json
{
  "code": 200,       // 业务状态码：200=成功，其他=失败
  "msg": "操作成功",  // 提示信息
  "data": { ... }    // 业务数据（分页时为 PageResult）
}
```

**分页响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 100,
    "records": [...],
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

> API 文档完整在线版本：启动后端服务后访问 `http://localhost:8080/doc.html`（Knife4j UI）

---

## 部署指南

### 方式一：Docker 一键部署（推荐）

确保已安装 Docker 和 Docker Compose，然后执行：

```bash
# 克隆项目后，在项目根目录执行
docker-compose up -d

# 查看容器状态
docker-compose ps

# 查看后端日志
docker-compose logs -f backend

# 查看前端日志
docker-compose logs -f frontend
```

> 容器启动顺序：MySQL(5s) -> Backend(15s) -> Frontend，完整启动约需 30 秒。

### 方式二：手动部署

#### 前置条件

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| Node.js | 18+ | 前端运行环境 |
| npm / yarn | latest | 前端包管理 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.x | 缓存（可选） |

#### 步骤 1：初始化数据库

```sql
-- 使用 root 用户登录 MySQL
mysql -u root -p

-- 执行初始化脚本
SOURCE /path/to/sql/v1.0__init.sql;

-- 执行增量脚本
SOURCE /path/to/sql/v1.1__operation_log.sql;
```

#### 步骤 2：配置后端

编辑 `backend/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: your_password_here

  # Redis 配置（可选，无 Redis 时删除此段，系统自动降级）
  redis:
    host: localhost
    port: 6379
    password: your_redis_password

  # WebSocket 配置
  websocket:
    port: 8080
    path: /ws/monitor

server:
  port: 8080

# JWT 配置（生产环境务必更换密钥！）
jwt:
  secret: your-256-bit-secret-key-change-in-production
  expiration: 86400000  # 24 小时（毫秒）

# MiniMax AI 配置（可选，不配置则 AI 功能不可用）
minimax:
  api-key: your_minimax_api_key
  api-url: https://api.us-west-2.modal.direct

# NL 规则配置路径
nl:
  rules-dir: classpath:nl-rules
```

#### 步骤 3：构建并启动后端

```bash
cd backend

# 编译打包（跳过单元测试以加快速度）
mvn clean package -DskipTests

# 运行
java -jar target/generic-sys-admin-1.0.0.jar --spring.profiles.active=dev
```

或开发模式运行（支持热重载）：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动后访问：`http://localhost:8080/doc.html` 查看在线 API 文档。

#### 步骤 4：构建并启动前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev
# 前端访问地址：http://localhost:5173

# 生产构建
npm run build
# 构建产物在 dist/ 目录，可部署至 Nginx
```

#### 步骤 5：Nginx 部署前端（生产环境）

```nginx
server {
    listen       80;
    server_name  your-domain.com;

    # 前端静态文件
    location / {
        root   /var/www/generic-sys-admin/dist;
        index  index.html;
        try_files $uri $uri/ /index.html;  # SPA 历史路由支持
    }

    # API 反向代理
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 反向代理
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    # 静态资源（后端）
    location /static {
        proxy_pass http://127.0.0.1:8080;
    }
}
```

### 默认账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 系统管理员（全部权限） |
| operator | 123456 | 操作员（设备/耗材管理权限） |
| viewer | 123456 | 查看者（只读权限） |

> **生产环境请立即修改默认密码！**

---

## 配置参考

### application-dev.yml 完整配置模板

```yaml
spring:
  application:
    name: generic-sys-admin

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD:your_password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000

  # Redis 配置（可选）
  # 删除此段则系统自动降级为无缓存模式
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 5000ms

  # WebSocket 配置
  websocket:
    path: /ws/monitor
    allowed-origins: "*"

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.zyy.model.entity
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /
  tomcat:
    threads:
      max: 200
      min-spare: 10

# JWT 配置（生产环境必须更换密钥！）
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production-abc123def456}
  expiration: ${JWT_EXPIRATION:86400000}
  header: Authorization
  prefix: "Bearer "

# NL 自然语言配置
nl:
  enabled: true
  rules-dir: classpath:nl-rules
  default-intent: QUERY

# Knife4j API 文档配置（生产环境建议关闭）
knife4j:
  enable: true
  setting:
    language: zh_cn

# MiniMax AI 服务配置（可选，不配置 AI 功能不可用）
minimax:
  api-key: ${MINIMAX_API_KEY:}
  api-url: ${MINIMAX_API_URL:https://api.us-west-2.modal.direct}

# 日志配置
logging:
  level:
    com.zyy: INFO
    com.zyy.mapper: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/generic-sys-admin.log
    max-size: 100MB
    max-history: 30

# 操作日志 AOP 配置
operation-log:
  enabled: true
  excludes: # 排除不记录日志的接口
    - /api/auth/login
    - /api/auth/captcha
    - /api/logs
    - /api/nl/execute
```

### 环境变量模板（生产部署推荐）

```bash
# 数据库
export DB_PASSWORD="your_s...word"

# Redis（可选）
export REDIS_HOST="redis-host"
export REDIS_PORT="6379"
export REDIS_PASSWORD="your_r...word"

# JWT
export JWT_SECRET="your-v...-key"

# 服务端口
export SERVER_PORT="8080"

# MiniMax AI（可选）
export MINIMAX_API_KEY="your_m..._key"
export MINIMAX_API_URL="https://api.us-west-2.modal.direct"
```

---

## 安全说明

1. **密码安全**：所有用户密码使用 BCrypt（cost factor = 10）加密存储，绝不以明文保存。
2. **JWT 安全**：令牌有效期 24 小时，敏感操作建议配合前端超时机制。
3. **账户锁定**：连续 5 次登录失败，账户锁定 30 分钟。
4. **SQL 注入防护**：使用 MyBatis-Plus 参数化查询，完全避免 SQL 注入风险。
5. **XSS 防护**：后端对输入进行基本过滤，前端 Element Plus 组件自带 XSS 防护。
6. **CORS 配置**：生产环境请在 `WebConfig` 中限制允许的跨域来源。
7. **日志脱敏**：响应数据中的敏感字段（如密码字段）在返回前会被置空。
8. **接口权限**：使用 `@PreAuthorize` 注解控制接口访问权限。

---

## NL业务流配置示例

### 设备查询规则

```yaml
# nl-rules/device-query.yaml
intent: QUERY_DEVICE
patterns:
  - "查询设备 {deviceCode}"
  - "查看设备 {deviceCode}"
  - "设备 {deviceCode} 的信息"
  - "我想看看 {deviceCode}"
entities:
  deviceCode:
    type: STRING
    required: true
    pattern: "^EQ-\\d{4}-\\d{3}$"
    errorHint: "设备编号格式应为 EQ-YYYY-NNN"
response:
  format: "detail"
  fields:
    - code
    - name
    - category
    - model
    - status
    - location
    - purchaseDate
    - maintenanceInfo
```

### 耗材入库规则

```yaml
# nl-rules/consumable-inbound.yaml
intent: CONSUMABLE_INBOUND
patterns:
  - "入库耗材 {productName} {quantity}{unit}"
  - "采购入库 {productName} {quantity}个"
  - "增加库存 {productName} {quantity}"
entities:
  productName:
    type: STRING
    required: true
  quantity:
    type: INTEGER
    required: true
    min: 1
  unit:
    type: STRING
    required: false
    default: "个"
validation:
  stockCheck:
    enabled: true
    maxStockLevel: consumable.maxStockLevel
response:
  format: "transaction"
  fields:
    - productName
    - quantity
    - balanceAfter
    - transactionTime
```

### 操作日志查询规则

```yaml
# nl-rules/log-query.yaml
intent: QUERY_LOG
patterns:
  - "查询我的操作日志"
  - "查看最近的日志"
  - "操作记录"
  - "谁在什么时候做了什么"
entities:
  operator:
    type: USER_REF
    required: false
    default: current
  timeRange:
    type: TIME_RANGE
    required: false
    default: "last_7_days"
  module:
    type: STRING
    required: false
response:
  format: "list"
  fields:
    - operationTime
    - username
    - module
    - operation
    - targetTable
    - resultStatus
```

---

## 常见问题

### Q: 如何添加新的NL指令类型？

1. 在 `nl-rules/` 目录下创建新的规则 YAML 文件
2. 定义 intent、patterns、entities
3. 在对应的 Service 中实现业务逻辑
4. 在 NL 规则引擎中注册新规则

### Q: 如何添加新的角色？

1. 在 `sys_role` 表中插入新角色记录
2. 在 `sys_role_menu` 中为角色分配菜单权限
3. 在 Spring Security 配置中添加新的权限表达式

### Q: WebSocket 连接失败怎么办？

1. 检查 Nginx 是否正确配置了 WebSocket 反向代理
2. 确认防火墙是否开放了对应端口
3. 检查前端 SockJS 版本兼容性

---

## 许可证

本项目基于 MIT 许可证开源，您可以自由使用、修改和分发，但请保留原作者署名。

---

## 联系方式

如有问题或建议，欢迎提交 Issue。