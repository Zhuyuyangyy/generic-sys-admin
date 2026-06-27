package com.zyy.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.zyy.common.TenantContext;
import com.zyy.security.DataScopeFilter;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * ==========================================================
 * MyBatis-Plus 全局配置
 * ==========================================================
 *
 * 配置内容：
 * 1. 逻辑删除（is_deleted 字段）
 * 2. 自动填充（create_time / update_time / is_deleted / tenantId）
 * 3. 多租户拦截器（TenantLineInnerInterceptor）
 * 4. 数据权限拦截器（DataPermissionInterceptor + DataScopeFilter）
 * 5. 分页拦截器（PaginationInnerInterceptor）
 *
 * @author Alice
 */
@Configuration
@MapperScan("com.zyy.**.mapper")
public class MyBatisPlusConfig {

    @Autowired
    private TenantLineInnerInterceptor tenantLineInnerInterceptor;

    @Autowired
    private DataScopeFilter dataScopeFilter;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Tenant interceptor must be first
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        // Data permission interceptor (ABAC data scope filtering)
        DataPermissionInterceptor dataPermissionInterceptor = new DataPermissionInterceptor();
        dataPermissionInterceptor.setDataPermissionHandler(dataScopeFilter);
        interceptor.addInnerInterceptor(dataPermissionInterceptor);
        // Pagination interceptor
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * MetaObjectHandler 自动填充
     *
     * 功能：插入记录时自动填充 create_time / is_deleted / tenant_id，更新记录时自动填充 update_time
     * 配合 BaseEntity 使用，开发者不需要关心这些字段，MyBatis-Plus 全自动处理
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
                // Auto-fill tenantId from TenantContext
                Long tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
