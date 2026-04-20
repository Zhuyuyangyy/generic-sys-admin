package com.zyy.service;

import com.zyy.model.dto.EquipmentSaveDTO;
import com.zyy.model.dto.EquipmentUpdateDTO;
import com.zyy.model.vo.EquipmentVO;
import com.zyy.model.vo.PageVO;

/**
 * Equipment business service interface.
 * <p>
 * Defines CRUD operations and status management for equipment assets.
 *
 * @author System Architect
 */
public interface EquipmentService {

    /**
     * Register a new equipment asset.
     *
     * @param saveDTO     Equipment data from presentation layer
     * @param operatorId  Creator user ID from security context
     * @return Created equipment view object
     */
    EquipmentVO save(EquipmentSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing equipment record.
     *
     * @param updateDTO   Updated equipment data
     * @param operatorId  Operator user ID for audit
     * @return Updated equipment view object
     */
    EquipmentVO update(EquipmentUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve a single equipment record by ID.
     *
     * @param id Equipment identifier
     * @return Equipment view object or null if not found
     */
    EquipmentVO getById(Long id);

    /**
     * Retrieve paginated equipment list with optional filters.
     *
     * @param pageNum   Page number (1-based)
     * @param pageSize  Items per page
     * @param name      Optional name filter (partial match)
     * @param category  Optional category filter (exact match)
     * @param status    Optional status filter
     * @return Paginated equipment list
     */
    PageVO<EquipmentVO> getPage(Long pageNum, Long pageSize, String name, String category, Integer status);

    /**
     * Update equipment operational status.
     * Enforces valid state transitions:
     * 1 (Normal) 鈫?0 (Maintenance)
     * 0 (Maintenance) 鈫?1 (Normal)
     * 1 (Normal) 鈫?2 (Scrapped) / 0 (Maintenance) 鈫?2 (Scrapped)
     *
     * @param id         Equipment identifier
     * @param newStatus  Target status
     * @param operatorId Operator user ID
     */
    void updateStatus(Long id, Integer newStatus, Long operatorId);

    /**
     * Remove an equipment record (soft delete).
     *
     * @param id         Equipment identifier
     * @param operatorId Operator user ID
     */
    void delete(Long id, Long operatorId);
}
