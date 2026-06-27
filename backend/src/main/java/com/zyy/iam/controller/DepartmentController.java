package com.zyy.iam.controller;

import com.zyy.common.Result;
import com.zyy.iam.model.dto.DepartmentSaveDTO;
import com.zyy.iam.model.dto.DepartmentUpdateDTO;
import com.zyy.iam.model.vo.DepartmentVO;
import com.zyy.iam.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for department management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "Department and organizational structure APIs")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Get department tree", description = "Retrieve department tree structure")
    public Result<List<DepartmentVO>> getTree() {
        List<DepartmentVO> tree = departmentService.getTree();
        return Result.ok(tree);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create department", description = "Create a new department")
    public Result<DepartmentVO> create(@Valid @RequestBody DepartmentSaveDTO saveDTO) {
        DepartmentVO dept = departmentService.create(saveDTO);
        return Result.ok(dept, "Department created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update department", description = "Update department information")
    public Result<DepartmentVO> update(
            @Parameter(description = "Department ID") @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateDTO updateDTO) {
        updateDTO.setId(id);
        DepartmentVO dept = departmentService.update(updateDTO);
        return Result.ok(dept, "Department updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete department", description = "Delete department (fails if has children)")
    public Result<Void> delete(
            @Parameter(description = "Department ID") @PathVariable Long id) {
        departmentService.delete(id);
        return Result.ok(null, "Department deleted successfully");
    }

    @GetMapping("/tree")
    @Operation(summary = "Get tree for selector", description = "Retrieve simplified department tree for dropdown selection")
    public Result<List<DepartmentVO>> getTreeForSelector() {
        List<DepartmentVO> tree = departmentService.getTreeForSelector();
        return Result.ok(tree);
    }
}
