package com.zyy.nl;

/**
 * 自然语言意图枚举。
 *
 * @author ZYY Agent
 * @since Java 17
 */
public enum NLIntent {
    /** 查询/检索 */
    QUERY,
    /** 创建实体 */
    CREATE,
    /** 更新实体 */
    UPDATE,
    /** 删除实体 */
    DELETE,
    /** 统计/汇总 */
    STATISTICS
}