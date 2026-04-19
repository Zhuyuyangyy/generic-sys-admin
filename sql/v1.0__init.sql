# =============================================
# 通用管理系统母版 · 数据库初始化脚本
# 数据库名: generic_sys_admin
# 编码: UTF8MB4
# =============================================

CREATE DATABASE IF NOT EXISTS `generic_sys_admin` 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `generic_sys_admin`;

-- ----------------------------
-- 1. 用户表 (sys_user)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`            BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
  `username`      VARCHAR(64)        NOT NULL                COMMENT '用户名，唯一标识',
  `password`      VARCHAR(255)       NOT NULL                COMMENT '加密后的密码（BCrypt）',
  `real_name`     VARCHAR(128)       DEFAULT NULL            COMMENT '真实姓名',
  `email`         VARCHAR(128)       DEFAULT NULL            COMMENT '电子邮箱',
  `phone`         VARCHAR(32)        DEFAULT NULL            COMMENT '手机号',
  `avatar_url`    VARCHAR(512)       DEFAULT NULL            COMMENT '头像URL',
  `status`        TINYINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT '账号状态：0=禁用，1=正常，2=锁定',
  `last_login_ip` VARCHAR(64)       DEFAULT NULL            COMMENT '最后登录IP',
  `last_login_at` DATETIME          DEFAULT NULL            COMMENT '最后登录时间',
  `create_user`   BIGINT UNSIGNED    DEFAULT NULL            COMMENT '创建人ID',
  `create_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0=未删，1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------
-- 2. 角色表 (sys_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`            BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键',
  `name`          VARCHAR(64)       NOT NULL                COMMENT '角色名称',
  `code`          VARCHAR(64)       NOT NULL                COMMENT '角色编码，唯一标识',
  `description`   VARCHAR(255)      DEFAULT NULL            COMMENT '角色描述',
  `status`        TINYINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT '状态：0=禁用，1=正常',
  `sort_order`    INT UNSIGNED      NOT NULL DEFAULT 0      COMMENT '排序序号',
  `create_user`   BIGINT UNSIGNED    DEFAULT NULL            COMMENT '创建人ID',
  `create_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- ----------------------------
-- 3. 用户-角色关联表 (sys_user_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`    BIGINT UNSIGNED  NOT NULL                COMMENT '用户ID',
  `role_id`    BIGINT UNSIGNED  NOT NULL                COMMENT '角色ID',
  `create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 4. 操作日志表 (sys_operation_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '日志ID，主键',
  `user_id`        BIGINT UNSIGNED  DEFAULT NULL            COMMENT '操作用户ID',
  `username`       VARCHAR(64)      DEFAULT NULL            COMMENT '操作用户名（冗余）',
  `module`         VARCHAR(128)     DEFAULT NULL            COMMENT '操作模块',
  `operation`      VARCHAR(64)      DEFAULT NULL            COMMENT '操作类型：INSERT/DELETE/UPDATE/SELECT/LOGIN',
  `target_table`   VARCHAR(128)     DEFAULT NULL            COMMENT '操作目标表名',
  `target_id`      VARCHAR(64)     DEFAULT NULL            COMMENT '操作目标记录ID',
  `method_name`    VARCHAR(256)     DEFAULT NULL            COMMENT 'Java方法全限定名',
  `request_params` TEXT            DEFAULT NULL            COMMENT '请求参数（JSON）',
  `request_method` VARCHAR(16)     DEFAULT NULL            COMMENT 'HTTP方法',
  `request_url`    VARCHAR(512)    DEFAULT NULL            COMMENT '请求URL',
  `ip_address`     VARCHAR(64)     DEFAULT NULL            COMMENT '客户端IP地址',
  `user_agent`     VARCHAR(512)    DEFAULT NULL            COMMENT 'User-Agent',
  `operation_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `duration_ms`    BIGINT UNSIGNED DEFAULT NULL            COMMENT '执行耗时（毫秒）',
  `result_status`  TINYINT UNSIGNED DEFAULT NULL            COMMENT '操作结果：0=失败，1=成功',
  `error_detail`   TEXT            DEFAULT NULL            COMMENT '异常信息/错误详情',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation` (`operation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表（AOP审计）';

-- ----------------------------
-- 5. 通用模板实体表 (template_entity)
-- ----------------------------
DROP TABLE IF EXISTS `template_entity`;
CREATE TABLE `template_entity` (
  `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '模板ID，主键',
  `name`            VARCHAR(128)     NOT NULL                COMMENT '模板名称',
  `code`            VARCHAR(64)      NOT NULL                COMMENT '模板编码，唯一标识',
  `category`        VARCHAR(64)      DEFAULT NULL            COMMENT '模板分类',
  `description`     VARCHAR(512)     DEFAULT NULL            COMMENT '模板描述',
  `schema_json`     JSON            DEFAULT NULL            COMMENT '数据结构Schema',
  `ui_config_json`  JSON            DEFAULT NULL            COMMENT 'UI配置',
  `preview_image`   VARCHAR(512)     DEFAULT NULL            COMMENT '预览图URL',
  `version`         VARCHAR(32)     NOT NULL DEFAULT '1.0.0' COMMENT '版本号',
  `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '状态：0=禁用，1=正常，2=草稿',
  `is_public`       TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '是否公开',
  `tags`            VARCHAR(512)    DEFAULT NULL            COMMENT '标签',
  `create_user_id`  BIGINT UNSIGNED  DEFAULT NULL            COMMENT '创建人ID',
  `create_user_name` VARCHAR(64)    DEFAULT NULL            COMMENT '创建人姓名',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id`       BIGINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT '租户ID',
  `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_tenant` (`code`, `tenant_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用模板实体表';

-- ----------------------------
-- 6. 菜单权限表 (sys_menu)
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '菜单ID，主键',
  `parent_id`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '父菜单ID，0为顶级',
  `name`        VARCHAR(64)      NOT NULL                COMMENT '菜单名称',
  `path`        VARCHAR(255)     DEFAULT NULL            COMMENT '路由路径',
  `component`   VARCHAR(255)     DEFAULT NULL            COMMENT '前端组件路径',
  `icon`        VARCHAR(64)     DEFAULT NULL            COMMENT '菜单图标',
  `sort_order`  INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '排序序号',
  `visible`     TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '是否显示：0=隐藏，1=显示',
  `is_external` TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '是否外链：0=否，1=是',
  `permission`  VARCHAR(128)    DEFAULT NULL            COMMENT '权限标识',
  `menu_type`   TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '菜单类型：1=目录，2=菜单，3=按钮',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单权限表';

-- ----------------------------
-- 7. 角色-菜单关联表 (sys_role_menu)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id`    BIGINT UNSIGNED  NOT NULL                COMMENT '角色ID',
  `menu_id`    BIGINT UNSIGNED  NOT NULL                COMMENT '菜单ID',
  `create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
-- 插入超级管理员角色
INSERT INTO `sys_role` (`name`, `code`, `description`, `status`) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有系统所有权限', 1),
('普通用户', 'USER', '普通用户权限', 1);

-- 插入默认用户 (密码: 123456, BCrypt加密后)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 1),
('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户', 1);

-- 绑定用户角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2);

-- 插入系统菜单
INSERT INTO `sys_menu` (`name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`) VALUES
('系统管理', '/system', NULL, 'Setting', 1, 1, NULL),
('用户管理', '/system/user', 'system/UserManage.vue', 'User', 10, 2, 'system:user:query'),
('角色管理', '/system/role', 'system/RoleManage.vue', 'UserFilled', 20, 2, 'system:role:query'),
('菜单管理', '/system/menu', 'system/MenuManage.vue', 'Menu', 30, 2, 'system:menu:query'),
('操作日志', '/system/log', 'system/LogManage.vue', 'Document', 40, 2, 'system:log:query'),
('数据大屏', '/dashboard', 'dashboard/DashboardView.vue', 'DataLine', 1, 2, 'dashboard:view'),
('语音播报', '/voice', 'voice/VoiceView.vue', 'Microphone', 50, 2, 'voice:use'),
('模板管理', '/template', 'template/TemplateList.vue', 'Grid', 60, 2, 'template:query');

-- 绑定角色菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `menu_type` = 2 AND `path` IN ('/dashboard');
