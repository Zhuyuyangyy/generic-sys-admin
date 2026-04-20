-- 操作日志表（sys_operation_log）
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `module` VARCHAR(100) DEFAULT '' COMMENT '操作模块',
  `operation_type` VARCHAR(50) DEFAULT '' COMMENT '操作类型',
  `description` VARCHAR(500) DEFAULT '' COMMENT '操作描述',
  `request_method` VARCHAR(10) DEFAULT '' COMMENT '请求方法',
  `request_url` VARCHAR(500) DEFAULT '' COMMENT '请求URL',
  `request_params` TEXT COMMENT '请求参数（JSON）',
  `status` TINYINT DEFAULT 1 COMMENT '响应状态：0=失败，1=成功',
  `error_msg` VARCHAR(1000) DEFAULT '' COMMENT '错误信息',
  `cost_time` BIGINT DEFAULT 0 COMMENT '耗时（毫秒）',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(100) DEFAULT '' COMMENT '操作人用户名',
  `operator_ip` VARCHAR(50) DEFAULT '' COMMENT '操作人IP',
  `user_agent` VARCHAR(500) DEFAULT '' COMMENT 'User-Agent',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_operator_id` (`operator_id`),
  INDEX `idx_module` (`module`),
  INDEX `idx_operation_type` (`operation_type`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';