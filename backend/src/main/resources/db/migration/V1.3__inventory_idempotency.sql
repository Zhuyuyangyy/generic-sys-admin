-- ===================================================================
-- V1.3 - Idempotency records for inventory write operations
-- ===================================================================
-- 库存的 inbound / outbound / adjustStock 都是增量操作（stock += delta）且
-- 每次都会插入一条 sys_inventory_transaction 流水。客户端超时重试会二次
-- 修改库存并产生幽灵流水，这是全系统唯一的"重复即破坏不变量"的写操作。
--
-- 本表只承载幂等控制，不承载业务语义：
--   (operator_id, operation, idempotency_key) 唯一 —— 同一调用方的同一操作
--   在同一 key 下只允许成功一次。数据库唯一约束是并发下的最后一道控制，
--   不依赖任何 JVM 内的 synchronized（那无法覆盖多实例部署）。
--
--   request_hash 存放决定副作用的业务字段指纹。同一 key 携带不同 payload
--   时判定为 key 被复用，返回 409，而不是静默执行第二次。
--
-- consumable_id / quantity 等属于 payload，进入 request_hash，【不】参与
-- 唯一键 —— 否则同一 key 换个耗材就能绕过唯一性。
-- ===================================================================

CREATE TABLE IF NOT EXISTS `sys_idempotency_record` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `operator_id`      BIGINT UNSIGNED NOT NULL                COMMENT 'Operator user ID (who triggered it)',
  `operation`        VARCHAR(64)     NOT NULL                COMMENT 'Logical operation: STOCK_INBOUND/STOCK_OUTBOUND/STOCK_ADJUST',
  `idempotency_key`  VARCHAR(128)    NOT NULL                COMMENT 'Client-supplied Idempotency-Key (opaque)',
  `request_hash`     CHAR(64)        NOT NULL                COMMENT 'SHA-256 of the canonical request payload',
  `status`           VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUCCESS / FAILED',
  `result_reference` VARCHAR(128)    DEFAULT NULL            COMMENT 'Business result reference, e.g. the ledger row id',
  `error_detail`     VARCHAR(512)    DEFAULT NULL            COMMENT 'Failure reason when status = FAILED',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Claim time',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last state change',
  PRIMARY KEY (`id`),
  -- 并发下的最终防线：同一 principal + 同一操作 + 同一 key 只能有一行。
  UNIQUE KEY `uk_idempotency` (`operator_id`, `operation`, `idempotency_key`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Idempotency records for inventory write operations';
