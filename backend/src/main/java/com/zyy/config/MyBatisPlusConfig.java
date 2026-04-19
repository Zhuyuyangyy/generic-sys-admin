package com.zyy.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * ==========================================================
 * 🏆 MyBatis-Plus 全局配置
 * ==========================================================
 *
 * 配置内容：
 * 1. 逻辑删除（is_deleted 字段）
 * 2. 自动填充（create_time / update_time / is_deleted）
 *
 * 为什么需要自动填充？
 * - 如果每次插入都用代码手动 setCreateTime，太累了
 * - 而且容易漏掉，数据库里时间字段变成 null 很丑
 * - 配置后：insert 时自动填充时间，更新时自动更新时间，逻辑删除自动标记
 *
 * @author Alice
 */
@Configuration
@MapperScan("com.zyy.**.mapper")
public class MyBatisPlusConfig {

    /**
     * 🏆 高级创新点：MetaObjectHandler 自动填充
     *
     * 功能：插入记录时自动填充 create_time / is_deleted，更新记录时自动填充 update_time
     * 配合 BaseEntity 使用，开发者不需要关心这些字段，MyBatis-Plus 全自动处理
     *
     * 使用步骤：
     * 1. 实体类字段标注 @TableField(fill = FieldFill.INSERT)        → createTime
     * 2. 实体类字段标注 @TableField(fill = FieldFill.INSERT_UPDATE) → updateTime
     * 3. 实体类字段标注 @TableField(fill = FieldFill.INSERT) + @TableLogic → isDeleted
     * 4. 这里配置自动填充规则
     *
     * 这样：
     * - 插入新记录 → createTime = NOW(), isDeleted = 0
     * - 更新记录   → updateTime = NOW()
     * - 删除记录   → isDeleted = 1（物理上没删，只是标记为删除）
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            /**
             * 插入时的填充规则
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                // 插入时：自动填充 createTime（如果实体类有这个字段）
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());

                // 插入时：自动填充 isDeleted = 0（如果实体类有这个字段）
                this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
            }

            /**
             * 更新时的填充规则
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // 更新时：自动填充 updateTime
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
