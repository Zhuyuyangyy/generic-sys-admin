package com.zyy.inventory.service;

import com.zyy.inventory.model.dto.ConsumableSaveDTO;
import com.zyy.inventory.model.dto.ConsumableUpdateDTO;
import com.zyy.inventory.model.vo.ConsumableVO;
import com.zyy.common.PageVO;

/**
 * Consumable inventory business service interface.
 *
 * @author System Architect
 */
public interface ConsumableService {

    /**
     * Register a new consumable product.
     */
    ConsumableVO save(ConsumableSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing consumable record.
     */
    ConsumableVO update(ConsumableUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve a consumable by ID.
     */
    ConsumableVO getById(Long id);

    /**
     * Retrieve paginated consumable list with optional filters.
     */
    PageVO<ConsumableVO> getPage(Long pageNum, Long pageSize, String name, String category, Integer status);

    /**
     * Adjust consumable stock quantity.
     * Triggers inventory transaction recording.
     *
     * @param id          Consumable ID
     * @param delta       Quantity change (+/-)
     * @param referenceNo Optional reference document number
     * @param remarks     Transaction remarks
     * @param operatorId  Operator user ID
     */
    void adjustStock(Long id, Integer delta, String referenceNo, String remarks, Long operatorId);

    /**
     * Perform inventory check-in (inbound).
     */
    void inbound(Long id, Integer quantity, String referenceNo, String remarks, Long operatorId);

    /**
     * Perform inventory check-out (outbound).
     * Validates sufficient stock before deduction.
     */
    void outbound(Long id, Integer quantity, String referenceNo, String remarks, Long operatorId);

    /**
     * Soft delete a consumable record.
     */
    void delete(Long id, Long operatorId);
}
