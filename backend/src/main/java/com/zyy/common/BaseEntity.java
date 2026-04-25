package com.zyy.generator;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ==========================================================
 * 🏆 高级创新点：通用基础实体类（BaseEntity）
 * ==========================================================
 *
 * 所有业务实体类都必须继承此类，即可自动获得：
 * 1. 雪花算法主键（分布式唯一ID，比自增ID更安全）
 * 2. 逻辑删除（is_deleted = 0 未删，= 1 已删，对业务透明）
 * 3. create_time / update_time 自动填充（由 MyBatis-Plus 自动维护）
 *
 * 为什么用雪花算法？
 * - 自增ID：危险！竞争对手爬取接口时直接遍历就知道你有多少数据
 * - 雪花ID：无意义的大数字，爬虫拿到也没用，更安全
 *
 * @author Alice
 */
@Data
public abstract class BaseEntity {

    /**
     * 主键 - 雪花算法生成
     * type = IdType.ASSIGN_ID 表示使用 MyBatis-Plus 的雪花算法
     * 这是默认配置，不用改
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间 - 自动填充
     * fill = FieldFill.INSERT 表示新增记录时自动填充
     * 配置在 MyBatisPlusConfig 中，详见该文件
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间 - 自动填充
     * fill = FieldFill.INSERT_UPDATE 表示新增和更新时都自动填充
     * 这样每次修改记录，update_time 都会自动更新
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记 ⚠️ 重要！
     * - is_deleted = 0 = 记录活着，正常查询时自动过滤
     * - is_deleted = 1 = 记录"假删除"，物理上还存在，但普通查询看不到
     * - 恢复数据：update is_deleted = 0 where id = xxx
     *
     * 为什么不用物理删除？
     * 1. 数据是资产，删了就没了，万一客户要恢复怎么办
     * 2. 审计需要，操作日志对不上会很麻烦
     * 3. 很多甲方合同要求数据保留 N 年，不能删
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
}
