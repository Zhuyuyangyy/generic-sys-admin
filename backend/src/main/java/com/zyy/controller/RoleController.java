package com.zyy.controller;

import com.zyy.common.PageParam;
import com.zyy.common.Result;
import com.zyy.model.entity.SysRoleEntity;
import com.zyy.model.vo.PageVO;
import com.zyy.model.vo.SysRoleVO;
import com.zyy.security.SecurityUtils;
import com.zyy.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;


import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "RBAC role management")
public class RoleController {

    private final SysRoleService roleService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("@ss.hasAuthority('system:role:list')")
    @Operation(summary = "分页查询角色")
    public Result<PageVO<SysRoleVO>> getPage(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return Result.ok(roleService.getPage(pageParam.getPageNum(), pageParam.getPageSize(), name, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('system:role:list')")
    @Operation(summary = "查询角色详情")
    public Result<SysRoleVO> getById(@PathVariable Long id) {
        return Result.ok(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasAuthority('system:role:add')")
    @Operation(summary = "新增角色")
    public Result<SysRoleVO> save(@Valid @RequestBody SysRoleEntity entity) {
        return Result.ok(roleService.save(entity, getCurrentUserId()), "角色创建成功");
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('system:role:edit')")
    @Operation(summary = "修改角色")
    public Result<SysRoleVO> update(@PathVariable Long id, @Valid @RequestBody SysRoleEntity entity) {
        entity.setId(id);
        return Result.ok(roleService.update(entity, getCurrentUserId()), "角色更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasAuthority('system:role:del')")
    @Operation(summary = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id, getCurrentUserId());
        return Result.ok(null, "角色删除成功");
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用角色（下拉框）")
    public Result<List<SysRoleVO>> getAllEnabled() {
        return Result.ok(roleService.getAllEnabled());
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("@ss.hasAuthority('system:role:grant')")
    @Operation(summary = "角色授权菜单")
    public Result<Void> grantMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.grantMenus(id, menuIds);
        return Result.ok(null, "菜单授权成功");
    }

    // ==================== Private ====================

    /**
     * 从 Spring Security Context 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        return securityUtils.currentUserId();
    }
}
