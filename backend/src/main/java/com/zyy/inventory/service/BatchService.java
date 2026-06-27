package com.zyy.inventory.service;

import com.zyy.inventory.model.dto.BatchSaveDTO;
import com.zyy.inventory.model.dto.BatchUpdateDTO;
import com.zyy.inventory.model.vo.BatchVO;
import com.zyy.common.PageVO;

import java.util.List;

/**
 * Consumable batch business service interface.
 *
 * @author System Architect
 */
public interface BatchService {

    /**
     * Create a new batch (inbound).
     *
     * @param saveDTO    Batch data from presentation layer
     * @param operatorId Creator user ID from security context
     * @return Created batch view object
     */
    BatchVO save(BatchSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing batch record.
     *
     * @param updateDTO  Updated batch data
     * @param operatorId Operator user ID for audit
     * @return Updated batch view object
     */
    BatchVO update(BatchUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve a single batch record by ID.
     *
     * @param id Batch identifier
     * @return Batch view object or null if not found
     */
    BatchVO getById(Long id);

    /**
     * Retrieve paginated batch list with optional filters.
     *
     * @param pageNum      Page number (1-based)
     * @param pageSize     Items per page
     * @param consumableId Optional consumable ID filter
     * @param status       Optional status filter (ACTIVE/EXPIRED/DEPLETED)
     * @return Paginated batch list
     */
    PageVO<BatchVO> getPage(Long pageNum, Long pageSize, Long consumableId, String status);

    /**
     * Update batch status.
     *
     * @param id         Batch identifier
     * @param status     Target status (ACTIVE/EXPIRED/DEPLETED)
     * @param operatorId Operator user ID
     */
    void updateStatus(Long id, String status, Long operatorId);

    /**
     * Get batches expiring within the specified number of days.
     *
     * @param days Number of days to look ahead
     * @return List of expiring batches
     */
    List<BatchVO> getExpiringBatches(int days);
}
