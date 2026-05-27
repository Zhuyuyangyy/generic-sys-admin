package com.zyy.causal;

/**
 * 因果机制类型枚举。
 * <p>描述因果图中边的语义类型，用于区分不同传播路径。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
public enum CausalMechanism {
    /** 外键依赖关系（如设备关联耗材） */
    FOREIGN_KEY,

    /** ETL数据转换关系（如数据清洗、格式转换） */
    ETL_TRANSFORM,

    /** 数据库Schema依赖（如表结构变更影响下游查询） */
    SCHEMA_DEPENDENCY,

    /** 质量传播（如数据质量逐层传递） */
    QUALITY_PROPAGATION,

    /** 级联故障（如某节点故障导致下游节点失效） */
    CASCADING_FAILURE
}
