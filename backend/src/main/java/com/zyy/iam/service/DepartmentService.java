package com.zyy.iam.service;

import com.zyy.iam.model.dto.DepartmentSaveDTO;
import com.zyy.iam.model.dto.DepartmentUpdateDTO;
import com.zyy.iam.model.vo.DepartmentVO;

import java.util.List;

/**
 * Department service interface for organizational structure management.
 */
public interface DepartmentService {

    /**
     * Get department tree structure.
     *
     * @return list of root departments with nested children
     */
    List<DepartmentVO> getTree();

    /**
     * Create a new department.
     *
     * @param saveDTO department creation data
     * @return created department view object
     */
    DepartmentVO create(DepartmentSaveDTO saveDTO);

    /**
     * Update a department.
     *
     * @param updateDTO department update data
     * @return updated department view object
     */
    DepartmentVO update(DepartmentUpdateDTO updateDTO);

    /**
     * Delete a department (soft delete).
     * Will fail if the department has children.
     *
     * @param id department identifier
     */
    void delete(Long id);

    /**
     * Get flat department tree for selector.
     *
     * @return simplified tree for dropdown selection
     */
    List<DepartmentVO> getTreeForSelector();
}
