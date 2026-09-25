package com.zyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.model.entity.IdempotencyRecordEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper for {@link IdempotencyRecordEntity}.
 *
 * <p>The insert is deliberately a plain SQL insert with no existence check: the
 * unique constraint on {@code (operator_id, operation, idempotency_key)} is the
 * arbiter. A check-then-insert would race under concurrency and let two requests
 * both believe they claimed the key.</p>
 *
 * @author System Architect
 */
@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecordEntity> {

    /**
     * Claim a key. On success the generated id is written back into
     * {@code record.id}, which is what the caller later uses to record the
     * outcome.
     *
     * @return 1 when this caller won the race, 0 when the row already exists
     */
    @Insert("INSERT INTO sys_idempotency_record "
            + "(operator_id, operation, idempotency_key, request_hash, status, created_at, updated_at) "
            + "VALUES (#{record.operatorId}, #{record.operation}, #{record.idempotencyKey}, "
            + "#{record.requestHash}, 'PENDING', NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "record.id")
    int claim(@Param("record") IdempotencyRecordEntity record);

    /**
     * Mark a claimed key as successfully completed, storing the business result
     * reference so a replayed request can report it.
     */
    @Update("UPDATE sys_idempotency_record SET status = 'SUCCESS', result_reference = #{resultReference}, "
            + "updated_at = NOW() WHERE id = #{id}")
    int markSuccess(@Param("id") Long id, @Param("resultReference") String resultReference);

    /**
     * Mark a claimed key as failed. The row stays so that the key cannot be
     * silently retried with a different outcome; see the service for the
     * documented retry semantics.
     */
    @Update("UPDATE sys_idempotency_record SET status = 'FAILED', error_detail = #{errorDetail}, "
            + "updated_at = NOW() WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("errorDetail") String errorDetail);

    /**
     * Delete a record only when it is FAILED. A SUCCESS row is never removed, so
     * a completed operation can always be replayed.
     */
    @Delete("DELETE FROM sys_idempotency_record "
            + "WHERE operator_id = #{operatorId} AND operation = #{operation} "
            + "AND idempotency_key = #{idempotencyKey} AND status = 'FAILED'")
    int deleteFailed(@Param("operatorId") Long operatorId,
                     @Param("operation") String operation,
                     @Param("idempotencyKey") String idempotencyKey);

    /**
     * Load a record scoped to exactly one principal, operation and key.
     */
    @Select("SELECT * FROM sys_idempotency_record "
            + "WHERE operator_id = #{operatorId} AND operation = #{operation} AND idempotency_key = #{idempotencyKey}")
    IdempotencyRecordEntity findScoped(@Param("operatorId") Long operatorId,
                                       @Param("operation") String operation,
                                       @Param("idempotencyKey") String idempotencyKey);
}
