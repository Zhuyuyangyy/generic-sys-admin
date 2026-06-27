package com.zyy.iam.service.impl;

import com.zyy.iam.mapper.SysMenuMapper;
import com.zyy.iam.mapper.SysRoleMapper;
import com.zyy.iam.mapper.SysUserRoleMapper;
import com.zyy.iam.model.entity.SysMenuEntity;
import com.zyy.iam.model.vo.SysMenuVO;
import com.zyy.iam.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public Set<String> getRolesByUserId(Long userId) {
        List<String> codes = roleMapper.selectCodeByUserId(userId);
        // 加上 ROLE_ 前缀（Spring Security 标准格式）
        return codes.stream()
                .map(code -> code.startsWith("ROLE_") ? code : "ROLE_" + code)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getPermissionsByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 根据角色获取菜单，提取 permission 字段
        List<SysMenuEntity> menus = menuMapper.selectByRoleIds(roleIds);
        Set<String> permissions = new HashSet<>();

        for (SysMenuEntity menu : menus) {
            String permission = menu.getPermission();
            if (permission != null && !permission.isBlank()) {
                // 优先使用菜单中存储的权限标识
                permissions.add(permission);
            } else if (menu.getPath() != null && !menu.getPath().isBlank()) {
                // 兜底：从菜单路径推导权限标识
                // 例如 /users → system:user:list, /equipment → equipment:view
                permissions.add(pathToPermission(menu.getPath()));
            }
        }

        return permissions;
    }

    @Override
    public List<SysMenuVO> getMenuTreeByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysMenuEntity> menus = menuMapper.selectByRoleIds(roleIds);
        List<SysMenuVO> allMenus = menus.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return buildTree(allMenus, 0L);
    }

    /**
     * 将菜单路径转换为权限标识
     * 规则：/users → system:user:list, /equipment → equipment:view
     */
    private String pathToPermission(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        // 去掉前导 /
        String clean = path.startsWith("/") ? path.substring(1) : path;
        // 最后一段作为操作类型
        String lastSegment = clean.contains("/")
                ? clean.substring(clean.lastIndexOf("/") + 1)
                : clean;
        // 首段作为系统前缀
        String firstSegment = clean.contains("/")
                ? clean.substring(0, clean.indexOf("/"))
                : clean;

        // 特殊模块映射
        return switch (firstSegment) {
            case "dashboard" -> "dashboard:view";
            case "users" -> "system:user:" + lastSegment;
            case "roles" -> "system:role:" + lastSegment;
            case "menus" -> "system:menu:" + lastSegment;
            case "equipment" -> "equipment:" + lastSegment;
            case "consumables" -> "consumable:" + lastSegment;
            case "inventory" -> "inventory:" + lastSegment;
            case "logs", "operation-logs" -> "system:log:" + lastSegment;
            default -> firstSegment + ":" + lastSegment;
        };
    }

    private List<SysMenuVO> buildTree(List<SysMenuVO> menus, Long parentId) {
        return menus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .peek(m -> {
                    List<SysMenuVO> children = buildTree(menus, m.getId());
                    if (!children.isEmpty()) {
                        m.setChildren(children);
                    }
                })
                .collect(Collectors.toList());
    }

    private SysMenuVO entityToVO(SysMenuEntity entity) {
        return SysMenuVO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .name(entity.getName())
                .path(entity.getPath())
                .component(entity.getComponent())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .visible(entity.getVisible())
                .permission(entity.getPermission())
                .menuType(entity.getMenuType())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
