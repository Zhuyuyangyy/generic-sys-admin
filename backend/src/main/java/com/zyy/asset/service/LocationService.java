package com.zyy.asset.service;

import com.zyy.asset.model.dto.LocationSaveDTO;
import com.zyy.asset.model.dto.LocationUpdateDTO;
import com.zyy.asset.model.vo.LocationVO;

import java.util.List;

/**
 * Location business service interface.
 * <p>
 * Defines CRUD operations and tree structure management for asset locations.
 *
 * @author System Architect
 */
public interface LocationService {

    /**
     * Create a new location.
     *
     * @param saveDTO    Location data from presentation layer
     * @param operatorId Creator user ID from security context
     * @return Created location view object
     */
    LocationVO save(LocationSaveDTO saveDTO, Long operatorId);

    /**
     * Update an existing location.
     *
     * @param updateDTO  Updated location data
     * @param operatorId Operator user ID for audit
     * @return Updated location view object
     */
    LocationVO update(LocationUpdateDTO updateDTO, Long operatorId);

    /**
     * Retrieve all locations as a tree structure.
     *
     * @return List of root location nodes with nested children
     */
    List<LocationVO> getTree();

    /**
     * Remove a location (soft delete).
     *
     * @param id         Location identifier
     * @param operatorId Operator user ID
     */
    void delete(Long id, Long operatorId);
}
