package com.zyy.inventory.service;

import com.zyy.inventory.model.dto.SupplierSaveDTO;
import com.zyy.inventory.model.dto.SupplierUpdateDTO;
import com.zyy.inventory.model.vo.SupplierVO;
import com.zyy.common.PageVO;

/**
 * Supplier business service interface.
 * <p>
 * Defines CRUD operations for consumable supplier management.
 *
 * @author System Architect
 */
public interface SupplierService {

    /**
     * Create a new supplier.
     *
     * @param saveDTO    Supplier data from presentation layer
     * @param operatorId Creator user ID from security context
     * @return Created supplier view object
     */
    SupplierVO save(SupplierSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing supplier.
     *
     * @param updateDTO  Updated supplier data
     * @param operatorId Operator user ID for audit
     * @return Updated supplier view object
     */
    SupplierVO update(SupplierUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve a single supplier by ID.
     *
     * @param id Supplier identifier
     * @return Supplier view object or null if not found
     */
    SupplierVO getById(Long id);

    /**
     * Retrieve paginated supplier list with optional filters.
     *
     * @param pageNum  Page number (1-based)
     * @param pageSize Items per page
     * @param name     Optional name filter (partial match)
     * @param status   Optional status filter
     * @return Paginated supplier list
     */
    PageVO<SupplierVO> getPage(Long pageNum, Long pageSize, String name, Integer status);

    /**
     * Remove a supplier (soft delete).
     *
     * @param id         Supplier identifier
     * @param operatorId Operator user ID
     */
    void delete(Long id, Long operatorId);
}
