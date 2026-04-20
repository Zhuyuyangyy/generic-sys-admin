package com.zyy.controller;

import com.zyy.common.Result;
import com.zyy.model.vo.SysMenuVO;
import com.zyy.security.LoginUser;
import com.zyy.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理接口
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "System menu management")
public class MenuController {

    private final SysMenuService menuService;

    /**
     * 获取当前用户的动态菜单树
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前用户菜单", description = "根据当前登录用户的角色返回动态菜单树")
    public Result<List<SysMenuVO>> getCurrentUserMenus() {
        Long userId = getCurrentUserId();
        List<SysMenuVO> menus = menuService.getMenusByUserId(userId);
        return Result.ok(menus);
    }

    /**
     * 获取所有菜单树（角色管理用）
     */
    @GetMapping("/tree")
    @PreAuthorize("@ss.hasAuthority('system:menu:list')")
    @Operation(summary = "获取所有菜单树")
    public Result<List<SysMenuVO>> getAllMenusTree() {
        List<SysMenuVO> menus = menuService.getAllMenusTree();
        return Result.ok(menus);
    }

    // ==================== Private ====================

    /**
     * 从 Spring Security Context 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.zyy.exception.UnauthorizedException("用户未登录");
        }
        if (authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        throw new com.zyy.exception.UnauthorizedException("无法获取用户信息");
    }
}