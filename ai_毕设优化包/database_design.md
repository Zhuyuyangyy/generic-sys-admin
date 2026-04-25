# 数据库设计文档

---

## 第一章 数据库设计概述

### 1.1 数据库选型

| 项目 | 说明 |
|------|------|
| 数据库类型 | 关系型数据库 |
| 数据库产品 | MySQL 8.0+ |
| 字符编码 | utf8mb4（支持emoji和特殊字符） |
| 排序规则 | utf8mb4_general_ci |
| ORM框架 | MyBatis-Plus 3.5.x |

### 1.2 数据库命名规范

- 库名/表名/字段名：全小写，单词间用下划线分隔
- 主键统一命名：`id`
- 外键命名：`<实体名>_id`
- 创建时间：`create_time`
- 更新时间：`update_time`
- 逻辑删除：`is_deleted`

---

## 第二章 E-R图（文字描述版）

### 2.1 完整E-R关系

```
                        ┌──────────────────┐
                        │    sys_menu     │
                        │   (菜单资源表)    │
                        │                  │
                        │ id (PK)          │
                        │ parent_id (FK)   │◄──────────┐
                        │ name             │            │
                        │ path             │            │
                        │ component        │            │
                        │ type             │            │
                        │ icon             │            │
                        │ sort             │            │
                        │ is_deleted       │            │
                        └───────┬──────────┘            │
                                │                        │
                           N:M │                        │ N:M
                                ▼                        ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   sys_user_role  │  │     sys_role      │  │  sys_role_menu   │
│  (用户角色关联表)  │  │    (系统角色表)   │  │  (角色菜单关联表) │
│                  │  │                  │  │                  │
│ user_id (FK,PK)  │◄─┤ id (PK)          ├─►│ role_id (FK,PK)  │
│ role_id (FK,PK)  │  │ role_name        │  │ menu_id (FK,PK)  │
│                  │  │ role_key         │  │                  │
└───────┬──────────┘  │ description      │  └──────────────────┘
        │              │ sort             │
        │              │ is_deleted       │
        │ 1:N          └───────┬──────────┘
        │                      │
        ▼                      │ N:1
┌──────────────────┐          │
│    sys_user      │──────────┘
│    (系统用户表)    │
│                  │
│ id (PK)          │
│ username (UK)    │
│ password         │
│ nickname         │
│ email            │
│ avatar           │
│ is_deleted       │
│ create_time      │
│ update_time      │
└───────┬──────────┘
        │
        │ 1:N
        ▼
┌──────────────────┐
│sys_operation_log │
│    (操作日志表)   │
│                  │
│ id (PK)          │
│ user_id (FK)     │
│ username         │
│ operation        │
│ method           │
│ params           │
│ ip               │
│ location         │
│ duration         │
│ status           │
│ error_msg        │
│ create_time      │
└──────────────────┘
```

### 2.2 E-R图说明

| 关系 | 说明 |
|------|------|
| 用户:N→N:角色 | 一个用户可以有多个角色，一个角色可以分配给多个用户 |
| 角色:N→N:菜单 | 一个角色可以访问多个菜单，一个菜单可以被多个角色访问 |
| 用户:1→N:日志 | 一个用户可以产生多条操作日志 |

---

## 第三章 数据表详细设计

### 3.1 sys_user（用户表）

**表说明：** 存储系统用户信息，包括登录凭证和个人资料。

```sql
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL        COMMENT '用户ID（雪花算法）',
    username        VARCHAR(50)     NOT NULL        COMMENT '用户名（唯一）',
    password        VARCHAR(100)    NOT NULL        COMMENT '密码（BCrypt加密）',
    nickname        VARCHAR(50)     DEFAULT NULL    COMMENT '昵称',
    email           VARCHAR(100)    DEFAULT NULL    COMMENT '邮箱',
    avatar          VARCHAR(500)    DEFAULT NULL    COMMENT '头像URL',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除：0-未删，1-已删',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';
```

**字段说明：**

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 雪花算法分布式ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名，用于登录 |
| password | VARCHAR(100) | NOT NULL | BCrypt加密后的密码 |
| nickname | VARCHAR(50) | - | 用户昵称 |
| email | VARCHAR(100) | - | 邮箱地址 |
| avatar | VARCHAR(500) | - | 头像图片URL |
| is_deleted | TINYINT | DEFAULT 0 | 逻辑删除标记 |

---

### 3.2 sys_role（角色表）

```sql
CREATE TABLE sys_role (
    id              BIGINT          NOT NULL        COMMENT '角色ID',
    role_name       VARCHAR(50)     NOT NULL        COMMENT '角色名称',
    role_key        VARCHAR(50)     NOT NULL        COMMENT '角色标识（英文唯一）',
    description     VARCHAR(255)    DEFAULT NULL    COMMENT '角色描述',
    sort            INT             DEFAULT 0       COMMENT '显示顺序',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key),
    KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统角色表';
```

**初始数据：**
```sql
INSERT INTO sys_role (id, role_name, role_key, description, sort) VALUES
(1, '超级管理员', 'admin', '拥有系统所有权限', 1),
(2, '普通用户', 'user', '普通用户权限', 2);
```

---

### 3.3 sys_user_role（用户角色关联表）

```sql
CREATE TABLE sys_user_role (
    user_id         BIGINT          NOT NULL        COMMENT '用户ID',
    role_id         BIGINT          NOT NULL        COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';
```

---

### 3.4 sys_menu（菜单资源表）

```sql
CREATE TABLE sys_menu (
    id              BIGINT          NOT NULL        COMMENT '菜单ID',
    parent_id       BIGINT          DEFAULT 0       COMMENT '父菜单ID（0=顶级）',
    name            VARCHAR(50)     NOT NULL        COMMENT '菜单名称',
    path            VARCHAR(200)    DEFAULT NULL    COMMENT '路由路径',
    component       VARCHAR(255)    DEFAULT NULL    COMMENT '前端组件路径',
    type            TINYINT        DEFAULT 1       COMMENT '类型：0-目录，1-菜单，2-按钮',
    icon            VARCHAR(50)    DEFAULT NULL    COMMENT '图标',
    sort            INT             DEFAULT 0       COMMENT '显示顺序',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统菜单表';
```

**初始数据：**
```sql
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort) VALUES
-- 顶级目录
(1, 0, '系统管理', '/system', NULL, 0, 'Setting', 1),
(2, 0, '数据大屏', '/dashboard', 'dashboard/DashboardView', 1, 'DataAnalysis', 2),
(3, 0, '用户管理', '/user', 'system/UserManage', 1, 'User', 3),
(4, 0, '角色管理', '/role', 'system/RoleManage', 1, 'Identity', 4);
```

---

### 3.5 sys_role_menu（角色菜单关联表）

```sql
CREATE TABLE sys_role_menu (
    role_id         BIGINT          NOT NULL        COMMENT '角色ID',
    menu_id         BIGINT          NOT NULL        COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';
```

---

### 3.6 sys_operation_log（操作日志表）

```sql
CREATE TABLE sys_operation_log (
    id              BIGINT          NOT NULL        COMMENT '日志ID',
    user_id         BIGINT          DEFAULT NULL    COMMENT '操作用户ID',
    username        VARCHAR(50)     DEFAULT NULL    COMMENT '操作用户名',
    operation       VARCHAR(100)    DEFAULT NULL    COMMENT '操作描述',
    method          VARCHAR(200)    DEFAULT NULL    COMMENT '请求方法全名',
    params          TEXT            DEFAULT NULL    COMMENT '请求参数（JSON）',
    ip              VARCHAR(50)     DEFAULT NULL    COMMENT '操作者IP地址',
    location        VARCHAR(100)    DEFAULT NULL    COMMENT '操作地点',
    duration        INT             DEFAULT 0       COMMENT '执行耗时（毫秒）',
    status          TINYINT        DEFAULT 1       COMMENT '状态：0-异常，1-正常',
    error_msg       TEXT            DEFAULT NULL    COMMENT '错误信息',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_username (username),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统操作日志表';
```

---

### 3.7 template_entity（通用实体母版）

```sql
CREATE TABLE template_entity (
    id              BIGINT          NOT NULL        COMMENT '主键ID',
    name            VARCHAR(100)    DEFAULT NULL    COMMENT '名称',
    description     TEXT            DEFAULT NULL    COMMENT '描述',
    status          TINYINT        DEFAULT 1       COMMENT '状态：1-正常，0-禁用',
    sort            INT             DEFAULT 0       COMMENT '排序',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_is_deleted (is_deleted),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用实体母版（供代码生成参考）';
```

---

## 第四章 索引设计

### 4.1 索引清单

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|------|--------|------|------|------|
| sys_user | PRIMARY | id | 主键 | - |
| sys_user | uk_username | username | 唯一索引 | 登录名唯一 |
| sys_user | idx_is_deleted | is_deleted | 普通索引 | 逻辑删除查询优化 |
| sys_role | PRIMARY | id | 主键 | - |
| sys_role | uk_role_key | role_key | 唯一索引 | 角色标识唯一 |
| sys_user_role | PRIMARY | user_id, role_id | 复合主键 | - |
| sys_user_role | idx_user_id | user_id | 普通索引 | 按用户查角色 |
| sys_user_role | idx_role_id | role_id | 普通索引 | 按角色查用户 |
| sys_menu | PRIMARY | id | 主键 | - |
| sys_menu | idx_parent_id | parent_id | 普通索引 | 树形查询优化 |
| sys_menu | idx_is_deleted | is_deleted | 普通索引 | - |
| sys_role_menu | PRIMARY | role_id, menu_id | 复合主键 | - |
| sys_operation_log | PRIMARY | id | 主键 | - |
| sys_operation_log | idx_user_id | user_id | 普通索引 | 按用户查日志 |
| sys_operation_log | idx_username | username | 普通索引 | - |
| sys_operation_log | idx_create_time | create_time | 普通索引 | 时间范围查询 |

### 4.2 索引优化建议

1. **日志表分区**：生产环境建议按月分区，避免单表数据量过大
2. **避免过度索引**：每个索引都会增加写操作的开销
3. **覆盖索引**：对于高频查询，考虑建立覆盖索引减少回表

---

## 第五章 完整建表SQL

```sql
-- ============================================================
-- 通用后台管理系统 - 数据库初始化脚本
-- 版本: v1.0
-- 创建时间: 2026-04-19
-- 说明: 包含7张核心表，自动创建索引和初始数据
-- ============================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS generic_sys_admin
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE generic_sys_admin;

-- ------------------------------------------------------------
-- 1. sys_user（用户表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL        COMMENT '用户ID（雪花算法）',
    username        VARCHAR(50)     NOT NULL        COMMENT '用户名（唯一）',
    password        VARCHAR(100)    NOT NULL        COMMENT '密码（BCrypt加密）',
    nickname        VARCHAR(50)     DEFAULT NULL    COMMENT '昵称',
    email           VARCHAR(100)    DEFAULT NULL    COMMENT '邮箱',
    avatar          VARCHAR(500)    DEFAULT NULL    COMMENT '头像URL',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除：0-未删，1-已删',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- 初始管理员账号（admin/123456，密码是BCrypt加密后的值）
INSERT INTO sys_user (id, username, password, nickname, email) VALUES
(1876543210123456789, 'admin', '$2a$10$xVFdGrKk3y1kGkY3e1XxXeJx8RqrZvVJp0kP0h6m3ZqXQqXqXqXq', '系统管理员', 'admin@example.com'),
(1876543210123456790, 'user', '$2a$10$xVFdGrKk3y1kGkY3e1XxXeJx8RqrZvVJp0kP0h6m3ZqXQqXqXqXq', '普通用户', 'user@example.com');

-- ------------------------------------------------------------
-- 2. sys_role（角色表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id              BIGINT          NOT NULL        COMMENT '角色ID',
    role_name       VARCHAR(50)     NOT NULL        COMMENT '角色名称',
    role_key        VARCHAR(50)     NOT NULL        COMMENT '角色标识（英文唯一）',
    description     VARCHAR(255)    DEFAULT NULL    COMMENT '角色描述',
    sort            INT             DEFAULT 0       COMMENT '显示顺序',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key),
    KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统角色表';

INSERT INTO sys_role (id, role_name, role_key, description, sort) VALUES
(1876543210123456788, '超级管理员', 'admin', '拥有系统所有权限', 1),
(1876543210123456787, '普通用户', 'user', '普通用户权限', 2);

-- ------------------------------------------------------------
-- 3. sys_user_role（用户角色关联表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id         BIGINT          NOT NULL        COMMENT '用户ID',
    role_id         BIGINT          NOT NULL        COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1876543210123456789, 1876543210123456788),  -- admin → 超级管理员
(1876543210123456790, 1876543210123456787);  -- user → 普通用户

-- ------------------------------------------------------------
-- 4. sys_menu（菜单资源表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id              BIGINT          NOT NULL        COMMENT '菜单ID',
    parent_id       BIGINT          DEFAULT 0       COMMENT '父菜单ID（0=顶级）',
    name            VARCHAR(50)     NOT NULL        COMMENT '菜单名称',
    path            VARCHAR(200)    DEFAULT NULL    COMMENT '路由路径',
    component       VARCHAR(255)    DEFAULT NULL    COMMENT '前端组件路径',
    type            TINYINT        DEFAULT 1       COMMENT '类型：0-目录，1-菜单，2-按钮',
    icon            VARCHAR(50)     DEFAULT NULL    COMMENT '图标',
    sort            INT             DEFAULT 0       COMMENT '显示顺序',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统菜单表';

INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort) VALUES
(1, 0, '系统管理', '/system', NULL, 0, 'Setting', 1),
(2, 0, '数据大屏', '/dashboard', 'dashboard/DashboardView', 1, 'DataAnalysis', 2),
(3, 1, '用户管理', '/user', 'system/UserManage', 1, 'User', 1),
(4, 1, '角色管理', '/role', 'system/RoleManage', 1, 'Identity', 2),
(5, 1, '菜单管理', '/menu', 'system/MenuManage', 1, 'Menu', 3),
(6, 1, '操作日志', '/log', 'system/OperationLog', 1, 'Document', 4);

-- ------------------------------------------------------------
-- 5. sys_role_menu（角色菜单关联表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    role_id         BIGINT          NOT NULL        COMMENT '角色ID',
    menu_id         BIGINT          NOT NULL        COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 超级管理员拥有所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1876543210123456788, id FROM sys_menu;

-- 普通用户只有数据大屏
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1876543210123456787, 2);

-- ------------------------------------------------------------
-- 6. sys_operation_log（操作日志表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id              BIGINT          NOT NULL        COMMENT '日志ID',
    user_id         BIGINT          DEFAULT NULL    COMMENT '操作用户ID',
    username        VARCHAR(50)     DEFAULT NULL    COMMENT '操作用户名',
    operation       VARCHAR(100)    DEFAULT NULL    COMMENT '操作描述',
    method          VARCHAR(200)    DEFAULT NULL    COMMENT '请求方法全名',
    params          TEXT            DEFAULT NULL    COMMENT '请求参数（JSON）',
    ip              VARCHAR(50)     DEFAULT NULL    COMMENT '操作者IP地址',
    location        VARCHAR(100)    DEFAULT NULL    COMMENT '操作地点',
    duration        INT             DEFAULT 0       COMMENT '执行耗时（毫秒）',
    status          TINYINT        DEFAULT 1       COMMENT '状态：0-异常，1-正常',
    error_msg       TEXT            DEFAULT NULL    COMMENT '错误信息',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_username (username),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统操作日志表';

-- ------------------------------------------------------------
-- 7. template_entity（通用实体母版）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS template_entity;
CREATE TABLE template_entity (
    id              BIGINT          NOT NULL        COMMENT '主键ID',
    name            VARCHAR(100)    DEFAULT NULL    COMMENT '名称',
    description     TEXT            DEFAULT NULL    COMMENT '描述',
    status          TINYINT        DEFAULT 1       COMMENT '状态：1-正常，0-禁用',
    sort            INT             DEFAULT 0       COMMENT '排序',
    is_deleted      TINYINT        DEFAULT 0       COMMENT '逻辑删除',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_is_deleted (is_deleted),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用实体母版（供代码生成参考）';

-- ============================================================
-- 执行完毕
-- ============================================================
```

---

*文档版本：v1.0*
*最后更新：2026-04-19*
