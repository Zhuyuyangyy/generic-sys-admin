package com.zyy.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.zyy.common.TenantContext;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Expression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus multi-tenant configuration.
 * Automatically appends AND tenant_id = ? to all SELECT queries
 * and sets tenant_id on INSERT operations.
 * Excludes sys_tenant table from tenant filtering.
 */
@Configuration
public class MyBatisPlusTenantConfig {

    /** Tables excluded from automatic tenant filtering */
    private static final Set<String> EXCLUDE_TABLES = Set.of(
            "sys_tenant"
    );

    @Bean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {

            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    return new LongValue(tenantId);
                }
                return new NullValue();
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // Exclude sys_tenant from tenant filtering
                if (EXCLUDE_TABLES.contains(tableName)) {
                    return true;
                }
                // If no tenant context, skip filtering (backward compatibility)
                return TenantContext.getTenantId() == null;
            }
        });
    }
}
