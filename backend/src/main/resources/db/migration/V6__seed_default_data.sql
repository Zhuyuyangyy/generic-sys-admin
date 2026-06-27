-- =============================================
-- V6: Seed Default Data
-- Default admin user, admin role, basic menus, test data
-- =============================================

-- ----------------------------
-- 1. Default roles
-- ----------------------------
INSERT IGNORE INTO `sys_role` (`id`, `name`, `code`, `description`, `status`, `sort_order`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有系统所有权限', 1, 1),
(2, '普通用户', 'USER', '普通用户权限', 1, 2);

-- ----------------------------
-- 2. Default users (password: 123456, BCrypt encoded)
-- BCrypt hash for "123456" with cost factor 10
-- ----------------------------
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKljEVMW', '系统管理员', 1),
(2, 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKljEVMW', '测试用户', 1);

-- ----------------------------
-- 3. Bind user-role associations
-- ----------------------------
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2);

-- ----------------------------
-- 4. System menus (Dashboard, Equipment, Consumables, System)
-- ----------------------------
-- Dashboard directory
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(1, 0, '数据大屏', '/dashboard', 'dashboard/DashboardView', 'DataLine', 1, 2, 'dashboard:view', 1);

-- Equipment management directory
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(2, 0, '设备管理', '/equipment', NULL, 'Monitor', 2, 1, NULL, 1),
(3, 2, '设备列表', '/equipment/list', 'equipment/EquipmentList', 'List', 10, 2, 'equipment:list', 1),
(4, 2, '设备台账', '/equipment/asset', 'equipment/AssetList', 'Tickets', 20, 2, 'equipment:asset', 1),
(5, 2, '维保计划', '/equipment/maintenance', 'equipment/MaintenancePlan', 'SetUp', 30, 2, 'equipment:maintenance', 1);

-- Consumables management directory
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(6, 0, '耗材管理', '/consumables', NULL, 'Box', 3, 1, NULL, 1),
(7, 6, '耗材列表', '/consumables/list', 'consumable/ConsumableList', 'List', 10, 2, 'consumable:list', 1),
(8, 6, '库存记录', '/consumables/inventory', 'consumable/InventoryRecord', 'Document', 20, 2, 'inventory:list', 1),
(9, 6, '库存预警', '/consumables/alert', 'consumable/StockAlert', 'Bell', 30, 2, 'inventory:alert', 1);

-- System management directory
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(10, 0, '系统管理', '/system', NULL, 'Setting', 4, 1, NULL, 1),
(11, 10, '用户管理', '/system/user', 'system/UserManage', 'User', 10, 2, 'system:user:list', 1),
(12, 10, '角色管理', '/system/role', 'system/RoleManage', 'UserFilled', 20, 2, 'system:role:list', 1),
(13, 10, '菜单管理', '/system/menu', 'system/MenuManage', 'Menu', 30, 2, 'system:menu:list', 1),
(14, 10, '操作日志', '/system/log', 'system/LogManage', 'Document', 40, 2, 'system:log:list', 1);

-- Button-level permissions for user management
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(15, 11, '用户新增', NULL, NULL, NULL, 1, 3, 'system:user:create', 1),
(16, 11, '用户修改', NULL, NULL, NULL, 2, 3, 'system:user:update', 1),
(17, 11, '用户删除', NULL, NULL, NULL, 3, 3, 'system:user:delete', 1);

-- Button-level permissions for role management
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(18, 12, '角色新增', NULL, NULL, NULL, 1, 3, 'system:role:create', 1),
(19, 12, '角色修改', NULL, NULL, NULL, 2, 3, 'system:role:update', 1),
(20, 12, '角色删除', NULL, NULL, NULL, 3, 3, 'system:role:delete', 1);

-- Button-level permissions for menu management
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(21, 13, '菜单新增', NULL, NULL, NULL, 1, 3, 'system:menu:create', 1),
(22, 13, '菜单修改', NULL, NULL, NULL, 2, 3, 'system:menu:update', 1),
(23, 13, '菜单删除', NULL, NULL, NULL, 3, 3, 'system:menu:delete', 1);

-- Audit log delete permission
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `status`) VALUES
(24, 14, '日志删除', NULL, NULL, NULL, 1, 3, 'audit:log:delete', 1);

-- ----------------------------
-- 5. Bind all menus to SUPER_ADMIN role
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `is_deleted` = 0;

-- Bind basic menus to USER role (dashboard only)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `is_deleted` = 0 AND `menu_type` = 2 AND `path` IN ('/dashboard');
