package com.zyy.common;

/**
 * ThreadLocal-based tenant context holder.
 * Stores the current tenant ID for the duration of a request.
 * Must be cleared after each request to prevent thread pool leakage.
 */
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Set the current tenant ID.
     *
     * @param tenantId tenant identifier
     */
    public static void setTenantId(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    /**
     * Get the current tenant ID.
     *
     * @return current tenant ID, or null if not set
     */
    public static Long getTenantId() {
        return TENANT_HOLDER.get();
    }

    /**
     * Clear the current tenant ID.
     * Must be called after each request to prevent thread pool leakage.
     */
    public static void clear() {
        TENANT_HOLDER.remove();
    }
}
