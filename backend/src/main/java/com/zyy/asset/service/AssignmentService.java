package com.zyy.asset.service;

import com.zyy.asset.model.dto.AssignmentSaveDTO;
import com.zyy.asset.model.vo.AssignmentVO;
import com.zyy.common.PageVO;

/**
 * Equipment assignment business service interface.
 *
 * @author System Architect
 */
public interface AssignmentService {

    /**
     * Assign equipment to a user.
     *
     * @param saveDTO    Assignment data from presentation layer
     * @param operatorId Creator user ID from security context
     * @return Created assignment view object
     */
    AssignmentVO save(AssignmentSaveDTO saveDTO, Long operatorId);

    /**
     * Return an assigned piece of equipment.
     *
     * @param id         Assignment identifier
     * @param operatorId Operator user ID
     * @return Updated assignment view object
     */
    AssignmentVO returnEquipment(Long id, Long operatorId);

    /**
     * Retrieve paginated assignment list with optional filters.
     *
     * @param pageNum     Page number (1-based)
     * @param pageSize    Items per page
     * @param equipmentId Optional equipment ID filter
     * @param userId      Optional user ID filter
     * @param status      Optional status filter (ACTIVE/RETURNED)
     * @return Paginated assignment list
     */
    PageVO<AssignmentVO> getPage(Long pageNum, Long pageSize, Long equipmentId, Long userId, String status);

    /**
     * Soft delete an assignment record.
     *
     * @param id         Assignment identifier
     * @param operatorId Operator user ID
     */
    void delete(Long id, Long operatorId);
}
