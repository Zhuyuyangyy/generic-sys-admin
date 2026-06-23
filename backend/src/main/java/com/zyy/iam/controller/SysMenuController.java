package com.zyy.iam.controller;

import com.zyy.common.Result;
import com.zyy.iam.model.dto.SysMenuSaveDTO;
import com.zyy.iam.model.dto.SysMenuUpdateDTO;
import com.zyy.iam.model.vo.SysMenuVO;
import com.zyy.iam.service.SysMenuService;
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
 * RESTful controller for menu management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "System menu and permission management APIs")
public class SysMenuController {

    private final SysMenuService menuService;

    @GetMapping
    @Operation(summary = "List menus", description = "Retrieve all menus in tree structure")
    public Result<List<SysMenuVO>> getMenuTree() {
        List<SysMenuVO> tree = menuService.getMenuTree();
        return Result.ok(tree);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get menu by ID", description = "Retrieve menu details by unique identifier")
    public Result<SysMenuVO> getById(
            @Parameter(description = "Menu ID") @PathVariable Long id) {
        SysMenuVO menu = menuService.getById(id);
        return Result.ok(menu);
    }

    @PostMapping
    @Operation(summary = "Create menu", description = "Create a new system menu")
    public Result<SysMenuVO> create(
            @Valid @RequestBody SysMenuSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        SysMenuVO menu = menuService.create(saveDTO, operatorId);
        return Result.ok(menu, "Menu created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu", description = "Update an existing menu")
    public Result<SysMenuVO> update(
            @Parameter(description = "Menu ID") @PathVariable Long id,
            @Valid @RequestBody SysMenuUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        SysMenuVO menu = menuService.update(updateDTO, operatorId);
        return Result.ok(menu, "Menu updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete menu", description = "Soft delete a menu (fails if menu has children)")
    public Result<Void> delete(
            @Parameter(description = "Menu ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        menuService.delete(id, operatorId);
        return Result.ok(null, "Menu deleted successfully");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return 1L;
    }
}
