package com.zyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.model.entity.ConsumableEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis-Plus Mapper for sys_consumable persistence operations.
 *
 * @author System Architect
 */
@Mapper
public interface ConsumableMapper extends BaseMapper<ConsumableEntity> {

    /**
     * 原子自增/自减库存。
     * <p>
     * 必须用 SQL 层 {@code SET stock = stock + delta}，而不是读出来改完再写回：
     * 后者是 read-modify-write，两个并发请求会互相覆盖（lost update）。
     * {@code stock >= 0} 作为 WHERE 条件一并执行，因此并发下也不可能扣成负数。
     * </p>
     *
     * @param id     consumable id
     * @param delta 正数入库，负数出库
     * @return 实际更新的行数，0 表示记录不存在或库存不足
     */
    @Update("UPDATE sys_consumable SET stock_quantity = stock_quantity + #{delta} " +
            "WHERE id = #{id} AND is_deleted = 0 AND stock_quantity + #{delta} >= 0")
    int adjustStockAtomic(@Param("id") Long id, @Param("delta") int delta);
}
