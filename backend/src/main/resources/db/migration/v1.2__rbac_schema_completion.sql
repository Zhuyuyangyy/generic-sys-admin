-- ===================================================================
-- V1.2 - RBAC schema completion
-- ===================================================================
-- SysMenuEntity 声明了 permission / menu_type / is_external 三个字段，
-- 但 V1.0 的 sys_menu 建表语句没有对应列。SysMenuMapper 使用 SELECT m.*，
-- 缺列时登录后的权限加载会直接抛 Unknown column。
--
-- 同时补齐按钮级权限标识：V1.0 只 seed 了 8 个顶层菜单，且没有 permission
-- 值，RbacServiceImpl 只能从 path 兜底推导（/users -> system:user:index），
-- 而 @PreAuthorize 需要的是 PermConst 定义的 system:user:list 等。
--
-- 本脚本没有 Flyway/Liquibase 来记录是否已应用，因此 ALTER 写成"仅当列不存在
-- 时才添加"，重复执行不报 Duplicate column name；后续 INSERT 全部带 NOT
-- EXISTS 保护，整体幂等。
-- ===================================================================

SET @has_permission := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_menu'
      AND COLUMN_NAME = 'permission'
);
SET @ddl_permission := IF(@has_permission = 0,
    'ALTER TABLE `sys_menu` ADD COLUMN `permission` VARCHAR(128) DEFAULT NULL COMMENT ''权限标识（如 system:user:list）'' AFTER `icon`',
    'SELECT 1');
PREPARE stmt FROM @ddl_permission; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_menu_type := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_menu'
      AND COLUMN_NAME = 'menu_type'
);
SET @ddl_menu_type := IF(@has_menu_type = 0,
    'ALTER TABLE `sys_menu` ADD COLUMN `menu_type` TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT ''菜单类型：1=目录，2=菜单，3=按钮'' AFTER `permission`',
    'SELECT 1');
PREPARE stmt FROM @ddl_menu_type; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_external := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_menu'
      AND COLUMN_NAME = 'is_external'
);
SET @ddl_external := IF(@has_external = 0,
    'ALTER TABLE `sys_menu` ADD COLUMN `is_external` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''是否外链：0=否，1=是'' AFTER `menu_type`',
    'SELECT 1');
PREPARE stmt FROM @ddl_external; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------
-- 给顶层菜单补权限标识（与 com.zyy.enums.PermConst 对齐）
-- -------------------------------------------------------------------
UPDATE `sys_menu` SET `permission` = 'dashboard:view'   WHERE `path` = '/dashboard';
UPDATE `sys_menu` SET `permission` = 'system:user:list' WHERE `path` = '/users';
UPDATE `sys_menu` SET `permission` = 'system:role:list' WHERE `path` = '/roles';
UPDATE `sys_menu` SET `permission` = 'system:menu:list' WHERE `path` = '/menus';
UPDATE `sys_menu` SET `permission` = 'equipment:list'   WHERE `path` = '/equipment';
UPDATE `sys_menu` SET `permission` = 'consumable:list'  WHERE `path` = '/consumables';
UPDATE `sys_menu` SET `permission` = 'inventory:list'   WHERE `path` = '/inventory';
UPDATE `sys_menu` SET `permission` = 'system:log:list'  WHERE `path` = '/logs';

-- -------------------------------------------------------------------
-- 按钮级权限（menu_type = 3），按父菜单挂载
-- -------------------------------------------------------------------
INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `component`, `icon`, `permission`, `menu_type`, `sort_order`, `visible`, `status`, `create_user`, `create_time`)
SELECT m.`id`, v.`name`, NULL, NULL, NULL, v.`permission`, 3, v.`sort_order`, 1, 1, 1, NOW()
FROM `sys_menu` m
JOIN (
    SELECT '/users'    AS `parent_path`, '查询' AS `name`, 'system:user:list'    AS `permission`, 1 AS `sort_order`
    UNION ALL SELECT '/users',    '新增', 'system:user:add',     2
    UNION ALL SELECT '/users',    '修改', 'system:user:edit',    3
    UNION ALL SELECT '/users',    '删除', 'system:user:del',     4
    UNION ALL SELECT '/roles',    '查询', 'system:role:list',    1
    UNION ALL SELECT '/roles',    '新增', 'system:role:add',     2
    UNION ALL SELECT '/roles',    '修改', 'system:role:edit',    3
    UNION ALL SELECT '/roles',    '删除', 'system:role:del',     4
    UNION ALL SELECT '/roles',    '授权', 'system:role:grant',   5
    UNION ALL SELECT '/menus',    '查询', 'system:menu:list',    1
    UNION ALL SELECT '/equipment',    '查询', 'equipment:list',   1
    UNION ALL SELECT '/equipment',    '新增', 'equipment:add',    2
    UNION ALL SELECT '/equipment',    '修改', 'equipment:edit',   3
    UNION ALL SELECT '/equipment',    '删除', 'equipment:del',    4
    UNION ALL SELECT '/equipment',    '详情', 'equipment:detail', 5
    UNION ALL SELECT '/consumables',  '查询', 'consumable:list',  1
    UNION ALL SELECT '/consumables',  '新增', 'consumable:add',   2
    UNION ALL SELECT '/consumables',  '修改', 'consumable:edit',  3
    UNION ALL SELECT '/consumables',  '删除', 'consumable:del',   4
    UNION ALL SELECT '/consumables',  '入库', 'consumable:in',    5
    UNION ALL SELECT '/consumables',  '出库', 'consumable:out',   6
    UNION ALL SELECT '/consumables',  '详情', 'consumable:detail',7
    UNION ALL SELECT '/inventory',    '查询', 'inventory:list',   1
    UNION ALL SELECT '/inventory',    '新增', 'inventory:add',    2
    UNION ALL SELECT '/inventory',    '修改', 'inventory:edit',   3
    UNION ALL SELECT '/inventory',    '删除', 'inventory:del',    4
    UNION ALL SELECT '/logs',         '查询', 'system:log:list',  1
) v ON m.`path` = v.`parent_path`
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` b
    WHERE b.`parent_id` = m.`id` AND b.`permission` = v.`permission`
);

-- -------------------------------------------------------------------
-- AI 工作室 / 文件存储 / 自然语言 的按钮级权限（menu_type = 3）
-- -------------------------------------------------------------------
INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `component`, `icon`, `permission`, `menu_type`, `sort_order`, `visible`, `status`, `create_user`, `create_time`)
SELECT 0, v.`name`, NULL, NULL, NULL, v.`permission`, 3, v.`sort_order`, 0, 1, 1, NOW()
FROM (
    SELECT '语音合成' AS `name`, 'ai:tts'      AS `permission`, 1 AS `sort_order`
    UNION ALL SELECT '图片生成', 'ai:image', 2
    UNION ALL SELECT '视频生成', 'ai:video', 3
    UNION ALL SELECT '文件上传', 'file:upload', 4
    UNION ALL SELECT '文件删除', 'file:del', 5
    UNION ALL SELECT '自然语言执行', 'nl:execute', 6
) v
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` b WHERE b.`permission` = v.`permission`
);

-- -------------------------------------------------------------------
-- 把新插入的按钮权限授给 Administrator（role_id = 1）
-- -------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`)
SELECT 1, `id`, NOW() FROM `sys_menu`
WHERE `permission` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = 1 AND rm.`menu_id` = `sys_menu`.`id`
);
