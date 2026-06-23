package com.zyy.iam.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.iam.model.dto.SysRoleSaveDTO;
import com.zyy.iam.model.dto.SysRoleUpdateDTO;
import com.zyy.iam.model.vo.SysMenuVO;
import com.zyy.iam.model.vo.SysRoleVO;
import com.zyy.iam.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for role management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "System role RBAC management APIs")
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    @Operation(summary = "List roles", description = "Retrieve paginated list of roles with optional filters")
    public Result<PageVO<SysRoleVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Role name filter (partial match)") @RequestParam(required = false) String roleName,
            @Parameter(description = "Status filter: 0=disabled, 1=normal") @RequestParam(required = false) Integer status) {
        PageVO<SysRoleVO> page = roleService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), roleName, status);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieve role details by unique identifier")
    public Result<SysRoleVO> getById(
            @Parameter(description = "Role ID") @PathVariable Long id) {
        SysRoleVO role = roleService.getById(id);
        return Result.ok(role);
    }

    @PostMapping
    @Operation(summary = "Create role", description = "Create a new system role")
    public Result<SysRoleVO> create(
            @Valid @RequestBody SysRoleSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        SysRoleVO role = roleService.create(saveDTO, operatorId);
        return Result.ok(role, "Role created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Update an existing role")
    public Result<SysRoleVO> update(
            @Parameter(description = "Role ID") @PathVariable Long id,
            @Valid @RequestBody SysRoleUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        SysRoleVO role = roleService.update(updateDTO, operatorId);
        return Result.ok(role, "Role updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Soft delete a role")
    public Result<Void> delete(
            @Parameter(description = "Role ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        roleService.delete(id, operatorId);
        return Result.ok(null, "Role deleted successfully");
    }

    @PostMapping("/{id}/menus")
    @Operation(summary = "Assign menus to role", description = "Assign menu permissions to a role")
    public Result<Void> assignMenus(
            @Parameter(description = "Role ID") @PathVariable Long id,
            @RequestBody List<Long> menuIds,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        roleService.assignMenus(id, menuIds, operatorId);
        return Result.ok(null, "Menus assigned successfully");
    }

    @GetMapping("/{id}/menus")
    @Operation(summary = "Get role menus", description = "Retrieve menus assigned to a role")
    public Result<List<SysMenuVO>> getRoleMenus(
            @Parameter(description = "Role ID") @PathVariable Long id) {
        List<SysMenuVO> menus = roleService.getRoleMenus(id);
        return Result.ok(menus);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return 1L;
    }
}
