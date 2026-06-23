CREATE TABLE IF NOT EXISTS `workflow_definition` (
  `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `definition_name` VARCHAR(128)     NOT NULL,
  `definition_code` VARCHAR(64)      NOT NULL,
  `description`     TEXT             DEFAULT NULL,
  `workflow_type`   VARCHAR(64)      NOT NULL,
  `steps`           TEXT             DEFAULT NULL COMMENT 'JSON array of approval steps',
  `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1,
  `version`         INT              NOT NULL DEFAULT 1,
  `create_user`     BIGINT UNSIGNED  DEFAULT NULL,
  `create_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_definition_code` (`definition_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Workflow definitions';

CREATE TABLE IF NOT EXISTS `workflow_instance` (
  `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `definition_id` BIGINT UNSIGNED  NOT NULL,
  `business_type` VARCHAR(64)      NOT NULL,
  `business_id`   BIGINT UNSIGNED  NOT NULL,
  `title`         VARCHAR(255)     NOT NULL,
  `initiator_id`  BIGINT UNSIGNED  NOT NULL,
  `current_step`  INT              NOT NULL DEFAULT 0,
  `total_steps`   INT              NOT NULL DEFAULT 1,
  `status`        VARCHAR(32)      NOT NULL DEFAULT 'PENDING',
  `create_user`   BIGINT UNSIGNED  DEFAULT NULL,
  `create_time`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Workflow instances';

CREATE TABLE IF NOT EXISTS `workflow_task` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `instance_id` BIGINT UNSIGNED  NOT NULL,
  `step_order`  INT              NOT NULL,
  `assignee_id` BIGINT UNSIGNED  NOT NULL,
  `action`      VARCHAR(32)      NOT NULL DEFAULT 'NONE',
  `comment`     TEXT             DEFAULT NULL,
  `status`      VARCHAR(32)      NOT NULL DEFAULT 'PENDING',
  `action_time` DATETIME         DEFAULT NULL,
  `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Workflow tasks';
