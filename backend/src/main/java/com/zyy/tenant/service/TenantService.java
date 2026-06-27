package com.zyy.tenant.service;

import com.zyy.common.PageVO;
import com.zyy.tenant.model.dto.TenantRegisterDTO;
import com.zyy.tenant.model.dto.TenantUpdateDTO;
import com.zyy.tenant.model.vo.TenantStatsVO;
import com.zyy.tenant.model.vo.TenantVO;

/**
 * Tenant management service interface.
 */
public interface TenantService {

    /**
     * Register a new tenant (super admin only).
     *
     * @param registerDTO tenant registration data
     * @return created tenant view object
     */
    TenantVO register(TenantRegisterDTO registerDTO);

    /**
     * Get paginated tenant list.
     *
     * @param pageNum  page number
     * @param pageSize page size
     * @param keyword  optional search keyword
     * @return paginated tenant list
     */
    PageVO<TenantVO> getPage(Long pageNum, Long pageSize, String keyword);

    /**
     * Get tenant by ID.
     *
     * @param id tenant identifier
     * @return tenant view object
     */
    TenantVO getById(Long id);

    /**
     * Update tenant information.
     *
     * @param updateDTO update data
     * @return updated tenant view object
     */
    TenantVO update(TenantUpdateDTO updateDTO);

    /**
     * Update tenant status (suspend/activate/terminate).
     *
     * @param id     tenant identifier
     * @param status new status (1=ACTIVE, 0=SUSPENDED, 2=TERMINATED)
     */
    void updateStatus(Long id, Integer status);

    /**
     * Get current user's tenant info.
     *
     * @param tenantId current user's tenant ID
     * @return tenant view object
     */
    TenantVO getCurrentTenant(Long tenantId);

    /**
     * Get tenant statistics.
     *
     * @param tenantId tenant identifier
     * @return tenant stats view object
     */
    TenantStatsVO getStats(Long tenantId);
}
