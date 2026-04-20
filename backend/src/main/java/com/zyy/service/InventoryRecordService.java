package com.zyy.service;

import com.zyy.model.dto.InventoryRecordSaveDTO;
import com.zyy.model.dto.InventoryRecordUpdateDTO;
import com.zyy.model.vo.InventoryRecordVO;
import com.zyy.model.vo.PageVO;

/**
 * Equipment maintenance and inspection record business service interface.
 *
 * @author System Architect
 */
public interface InventoryRecordService {

    /**
     * Create a new maintenance/inspection record.
     *
     * @param saveDTO     Record data
     * @param operatorId  Creator user ID
     * @return Created record view object
     */
    InventoryRecordVO save(InventoryRecordSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing record.
     *
     * @param updateDTO  Updated record data
     * @param operatorId Operator user ID
     * @return Updated record view object
     */
    InventoryRecordVO update(InventoryRecordUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve a single record by ID.
     *
     * @param id Record identifier
     * @return Record view object or null
     */
    InventoryRecordVO getById(Long id);

    /**
     * Retrieve paginated records with optional filters.
     *
     * @param pageNum      Page number
     * @param pageSize     Items per page
     * @param equipmentId  Optional equipment filter
     * @param recordType   Optional record type filter
     * @param startDate    Optional start date range filter
     * @param endDate      Optional end date range filter
     * @return Paginated record list
     */
    PageVO<InventoryRecordVO> getPage(Long pageNum, Long pageSize, Long equipmentId,
                                        String recordType, String startDate, String endDate);

    /**
     * Delete a maintenance record.
     *
     * @param id         Record identifier
     * @param operatorId Operator user ID
     */
    void delete(Long id, Long operatorId);

    /**
     * Get maintenance history for a specific equipment.
     *
     * @param equipmentId Equipment identifier
     * @param limit       Maximum number of records
     * @return List of recent maintenance records
     */
    PageVO<InventoryRecordVO> getByEquipment(Long equipmentId, Long pageNum, Long pageSize);
}
