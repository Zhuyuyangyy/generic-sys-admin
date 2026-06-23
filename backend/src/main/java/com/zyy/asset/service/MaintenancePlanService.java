package com.zyy.asset.service;

import com.zyy.asset.model.dto.MaintenancePlanSaveDTO;
import com.zyy.asset.model.dto.MaintenancePlanUpdateDTO;
import com.zyy.asset.model.vo.MaintenancePlanVO;
import com.zyy.common.PageVO;

/**
 * Maintenance plan business service interface.
 * <p>
 * Defines CRUD operations and status management for equipment maintenance plans.
 *
 * @author System Architect
 */
public interface MaintenancePlanService {

    /**
     * Create a new maintenance plan.
     *
     * @param saveDTO    Plan data from presentation layer
     * @param operatorId Creator user ID from security context
     * @return Created maintenance plan view object
     */
    MaintenancePlanVO save(MaintenancePlanSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing maintenance plan.
     *
     * @param updateDTO  Updated plan data
     * @param operatorId Operator user ID for audit
     * @return Updated maintenance plan view object
     */
    MaintenancePlanVO update(MaintenancePlanUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve a single maintenance plan by ID.
     *
     * @param id Plan identifier
     * @return Maintenance plan view object or null if not found
     */
    MaintenancePlanVO getById(Long id);

    /**
     * Retrieve paginated maintenance plan list with optional filters.
     *
     * @param pageNum     Page number (1-based)
     * @param pageSize    Items per page
     * @param equipmentId Optional equipment ID filter
     * @param status      Optional status filter
     * @return Paginated maintenance plan list
     */
    PageVO<MaintenancePlanVO> getPage(Long pageNum, Long pageSize, Long equipmentId, String status);

    /**
     * Update maintenance plan status.
     *
     * @param id         Plan identifier
     * @param status     Target status
     * @param operatorId Operator user ID
     */
    void updateStatus(Long id, String status, Long operatorId);

    /**
     * Remove a maintenance plan (soft delete).
     *
     * @param id         Plan identifier
     * @param operatorId Operator user ID
     */
    void delete(Long id, Long operatorId);
}
