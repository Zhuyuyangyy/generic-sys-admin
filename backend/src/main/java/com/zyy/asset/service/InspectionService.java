package com.zyy.asset.service;

import com.zyy.asset.model.dto.InspectionSaveDTO;
import com.zyy.asset.model.dto.InspectionUpdateDTO;
import com.zyy.asset.model.vo.InspectionVO;
import com.zyy.common.PageVO;

/**
 * Equipment inspection business service interface.
 *
 * @author System Architect
 */
public interface InspectionService {

    /**
     * Create a new inspection record.
     *
     * @param saveDTO    Inspection data from presentation layer
     * @param operatorId Creator user ID from security context
     * @return Created inspection view object
     */
    InspectionVO save(InspectionSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing inspection record.
     *
     * @param updateDTO  Updated inspection data
     * @param operatorId Operator user ID for audit
     * @return Updated inspection view object
     */
    InspectionVO update(InspectionUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve a single inspection record by ID.
     *
     * @param id Inspection identifier
     * @return Inspection view object or null if not found
     */
    InspectionVO getById(Long id);

    /**
     * Retrieve paginated inspection list with optional filters.
     *
     * @param pageNum     Page number (1-based)
     * @param pageSize    Items per page
     * @param equipmentId Optional equipment ID filter
     * @param result      Optional result filter (PASS/FAIL/CONDITIONAL)
     * @return Paginated inspection list
     */
    PageVO<InspectionVO> getPage(Long pageNum, Long pageSize, Long equipmentId, String result);

    /**
     * Soft delete an inspection record.
     *
     * @param id         Inspection identifier
     * @param operatorId Operator user ID
     */
    void delete(Long id, Long operatorId);
}
