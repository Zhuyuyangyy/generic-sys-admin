package com.zyy.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Idempotency record for the inventory write operations.
 *
 * <p>One row per (operator, operation, idempotency key) triple, which carries a
 * unique constraint. That constraint — not any JVM lock — is what makes a
 * duplicate request safe across multiple instances.</p>
 *
 * <p>The record is claimed as {@code PENDING} inside the same transaction as the
 * business mutation. If the transaction rolls back, both the claim and the stock
 * change disappear together, so a retry is not blocked by a half-written row.</p>
 *
 * @author System Architect
 */
@Data
@TableName("sys_idempotency_record")
public class IdempotencyRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Operator user ID — part of the key scope. */
    private Long operatorId;

    /** Logical operation, e.g. {@code STOCK_INBOUND}. */
    private String operation;

    /** Client-supplied opaque Idempotency-Key. */
    private String idempotencyKey;

    /** SHA-256 of the canonical request payload (hex, 64 chars). */
    private String requestHash;

    /** PENDING while the transaction is in flight, SUCCESS once committed. */
    private String status;

    /** Reference to the business result, e.g. the ledger row id. */
    private String resultReference;

    /** Failure reason when {@link #status} is FAILED. */
    private String errorDetail;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** Kept for symmetry with the other entities; rows here are never soft-deleted. */
    @TableField(exist = false)
    private Integer isDeleted;
}
