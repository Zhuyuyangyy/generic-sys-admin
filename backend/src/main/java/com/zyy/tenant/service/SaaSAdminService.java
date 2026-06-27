package com.zyy.tenant.service;

import com.zyy.common.PageVO;
import com.zyy.tenant.model.vo.PlatformStatsVO;
import com.zyy.tenant.model.vo.TenantUsageVO;
import com.zyy.tenant.model.vo.TenantVO;

/**
 * SaaS platform administration service.
 * Provides platform-level tenant management and usage tracking.
 */
public interface SaaSAdminService {

    /**
     * List all tenants with their statistics.
     *
     * @param pageNum  page number
     * @param pageSize page size
     * @param keyword  optional search keyword
     * @return paginated tenant list with stats
     */
    PageVO<TenantVO> listTenantsWithStats(Long pageNum, Long pageSize, String keyword);

    /**
     * Get platform-wide statistics.
     *
     * @return platform statistics
     */
    PlatformStatsVO getPlatformStats();

    /**
     * Suspend a tenant.
     *
     * @param tenantId tenant identifier
     */
    void suspendTenant(Long tenantId);

    /**
     * Activate a tenant.
     *
     * @param tenantId tenant identifier
     */
    void activateTenant(Long tenantId);

    /**
     * Get tenant usage statistics.
     *
     * @param tenantId tenant identifier
     * @return tenant usage information
     */
    TenantUsageVO getTenantUsage(Long tenantId);

    /**
     * Check if tenant can create a new user (subscription limit check).
     *
     * @param tenantId tenant identifier
     * @throws com.zyy.exception.BusinessException if limit exceeded
     */
    void checkUserLimit(Long tenantId);

    /**
     * Check if tenant can create a new asset (subscription limit check).
     *
     * @param tenantId tenant identifier
     * @throws com.zyy.exception.BusinessException if limit exceeded
     */
    void checkAssetLimit(Long tenantId);

    /**
     * Check if tenant can upload more files (storage limit check).
     *
     * @param tenantId    tenant identifier
     * @param fileSizeBytes size of the file to upload
     * @throws com.zyy.exception.BusinessException if limit exceeded
     */
    void checkStorageLimit(Long tenantId, long fileSizeBytes);
}
