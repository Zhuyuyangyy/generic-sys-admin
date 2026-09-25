package com.zyy.service;

import com.zyy.mapper.IdempotencyRecordMapper;
import com.zyy.model.entity.IdempotencyRecordEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exactly-once enforcement for the inventory write operations.
 *
 * <p>Why this exists: {@code ConsumableServiceImpl.adjustStock} is an increment
 * ({@code stock += delta}) and writes a ledger row. A client that times out and
 * retries changes the balance twice and grows a phantom ledger row. That is the
 * only place in the codebase where a duplicate request breaks an invariant.</p>
 *
 * <h3>Key scope</h3>
 * {@code (operator_id, operation, idempotency_key)}. The consumable id and the
 * quantity are payload — they go into the request fingerprint, never into the
 * unique key. Otherwise a caller could reuse one key across different
 * consumables and the constraint would happily allow both.
 *
 * <h3>Concurrency</h3>
 * The claim is a plain insert that relies on the unique constraint. There is no
 * check-then-insert and no {@code synchronized}; the database is the arbiter, so
 * this holds across multiple instances. The loser of the race receives
 * {@link Outcome#ALREADY_PROCESSING} (the winner has not yet committed).
 *
 * <h3>Transaction boundary</h3>
 * The claim happens inside the caller's transaction, together with the stock
 * update and the ledger insert. A rollback removes all three, so a retry after
 * a failure is never blocked by a half-written claim.
 *
 * <h3>Replay</h3>
 * Same key + same fingerprint on a committed request returns
 * {@link Outcome#ALREADY_SUCCEEDED} with the original result reference; the
 * business mutation is not repeated. Same key + different fingerprint returns
 * {@link Outcome#KEY_CONFLICT}, which the controller maps to 409.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryIdempotencyService {

    /** Logical operations, used as the middle component of the key scope. */
    public static final String OP_INBOUND = "STOCK_INBOUND";
    public static final String OP_OUTBOUND = "STOCK_OUTBOUND";
    public static final String OP_ADJUST = "STOCK_ADJUST";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    /** Maximum accepted Idempotency-Key length; the column is VARCHAR(128). */
    public static final int MAX_KEY_LENGTH = 128;

    private final IdempotencyRecordMapper recordMapper;

    /** Outcome of trying to take ownership of a key. */
    public enum Outcome {
        /** This caller owns the key and must run the business mutation. */
        CLAIMED,
        /** Another request owns the key and has not committed yet. */
        ALREADY_PROCESSING,
        /** A previous request with this key succeeded; resultReference is set. */
        ALREADY_SUCCEEDED,
        /** A previous request with this key failed; errorDetail is set. */
        PREVIOUSLY_FAILED,
        /** The key exists but the payload differs — the key was reused. */
        KEY_CONFLICT
    }

    /**
     * Result of a claim attempt. {@code outcome} is always present; the other
     * fields are populated depending on the outcome.
     */
    public record Claim(Outcome outcome, Long recordId, String resultReference, String errorDetail) {

        static Claim claimed(Long recordId) {
            return new Claim(Outcome.CLAIMED, recordId, null, null);
        }

        static Claim processing() {
            return new Claim(Outcome.ALREADY_PROCESSING, null, null, null);
        }

        static Claim succeeded(String resultReference) {
            return new Claim(Outcome.ALREADY_SUCCEEDED, null, resultReference, null);
        }

        static Claim failed(String errorDetail) {
            return new Claim(Outcome.PREVIOUSLY_FAILED, null, null, errorDetail);
        }

        static Claim conflict() {
            return new Claim(Outcome.KEY_CONFLICT, null, null, null);
        }
    }

    /** Thrown when a key is reused with a different payload. Maps to 409. */
    public static class IdempotencyKeyConflictException extends RuntimeException {
        public IdempotencyKeyConflictException(String message) {
            super(message);
        }
    }

    /**
     * Build the canonical fingerprint of a request.
     *
     * <p>Only fields that decide the side effect are included, in a fixed order.
     * Timestamps, trace ids, header order and JSON property order are excluded,
     * so two representations of the same business request hash identically.</p>
     *
     * @param operation  logical operation name
     * @param fields     ordered payload fields (names must be stable)
     */
    public static String fingerprint(String operation, Map<String, ?> fields) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("operation", operation);
        fields.forEach((k, v) -> canonical.put(k, v == null ? "" : v.toString()));
        StringBuilder sb = new StringBuilder();
        canonical.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Try to take ownership of {@code (operatorId, operation, idempotencyKey)}.
     *
     * <p>Must be called inside the caller's transaction, before any business
     * mutation. On {@link Outcome#CLAIMED} the caller must run the mutation and
     * then call {@link #markSuccess} or {@link #markFailed}.</p>
     *
     * @param keyLength the caller is expected to reject or accept long keys before
     *                  calling; see {@link #isKeyLengthAcceptable(String)}
     */
    public Claim claim(Long operatorId, String operation, String idempotencyKey, String requestHash) {
        IdempotencyRecordEntity claim = new IdempotencyRecordEntity();
        claim.setOperatorId(operatorId);
        claim.setOperation(operation);
        claim.setIdempotencyKey(idempotencyKey);
        claim.setRequestHash(requestHash);

        try {
            recordMapper.claim(claim);
            return Claim.claimed(claim.getId());
        } catch (DuplicateKeyException e) {
            // Lost the race, or a previous request owns the key. Inspect below.
            log.debug("Idempotency key already present: op={} key={}", operation, idempotencyKey);
        }

        IdempotencyRecordEntity existing =
                recordMapper.findScoped(operatorId, operation, idempotencyKey);
        if (existing == null) {
            // The winner rolled back between our insert and this read, so the
            // key is free again. Try once more rather than reporting a phantom
            // conflict.
            IdempotencyRecordEntity retry = new IdempotencyRecordEntity();
            retry.setOperatorId(operatorId);
            retry.setOperation(operation);
            retry.setIdempotencyKey(idempotencyKey);
            retry.setRequestHash(requestHash);
            try {
                recordMapper.claim(retry);
                return Claim.claimed(retry.getId());
            } catch (DuplicateKeyException e) {
                existing = recordMapper.findScoped(operatorId, operation, idempotencyKey);
            }
            if (existing == null) {
                return Claim.processing();
            }
        }

        if (!requestHash.equals(existing.getRequestHash())) {
            return Claim.conflict();
        }
        return switch (existing.getStatus() == null ? STATUS_PENDING : existing.getStatus()) {
            case STATUS_SUCCESS -> Claim.succeeded(existing.getResultReference());
            case STATUS_FAILED -> Claim.failed(existing.getErrorDetail());
            default -> Claim.processing();
        };
    }

    /** Record success and the business result reference. */
    public void markSuccess(Long recordId, String resultReference) {
        if (recordId != null) {
            recordMapper.markSuccess(recordId, resultReference);
        }
    }

    /** Record failure, keeping the row so the key is not silently retryable. */
    public void markFailed(Long recordId, String errorDetail) {
        if (recordId != null) {
            recordMapper.markFailed(recordId, errorDetail);
        }
    }

    /**
     * Delete a FAILED record so its key can be claimed again.
     *
     * <p>A failed request's transaction rolled back, so the business effect never
     * happened — but the PENDING row it claimed would otherwise sit in the way
     * (the unique constraint counts it). Removing exactly the FAILED row for this
     * scope keeps a retry possible without ever resurrecting a SUCCESS row.</p>
     */
    public void deleteFailed(Long operatorId, String operation, String idempotencyKey) {
        recordMapper.deleteFailed(operatorId, operation, idempotencyKey);
    }

    /** Whether a supplied key fits the column and is not blank. */
    public static boolean isKeyLengthAcceptable(String key) {
        return key != null && !key.isBlank() && key.length() <= MAX_KEY_LENGTH;
    }
}
