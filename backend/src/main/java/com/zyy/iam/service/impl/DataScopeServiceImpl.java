package com.zyy.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.common.TenantContext;
import com.zyy.iam.mapper.DataScopeMapper;
import com.zyy.iam.mapper.DepartmentMapper;
import com.zyy.iam.model.entity.DataScopeEntity;
import com.zyy.iam.service.DataScopeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Data scope service implementation for ABAC data permissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements DataScopeService {

    private final DataScopeMapper dataScopeMapper;
    private final DepartmentMapper departmentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getDataScope(String roleCode) {
        DataScopeEntity entity = dataScopeMapper.selectOne(
                new LambdaQueryWrapper<DataScopeEntity>()
                        .eq(DataScopeEntity::getRoleCode, roleCode)
                        .eq(DataScopeEntity::getIsDeleted, 0)
        );
        return entity != null ? entity.getScopeType() : "SELF";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDataScope(String roleCode, String scopeType, String customScope) {
        DataScopeEntity entity = dataScopeMapper.selectOne(
                new LambdaQueryWrapper<DataScopeEntity>()
                        .eq(DataScopeEntity::getRoleCode, roleCode)
                        .eq(DataScopeEntity::getIsDeleted, 0)
        );

        if (entity == null) {
            entity = new DataScopeEntity();
            entity.setRoleCode(roleCode);
            entity.setScopeType(scopeType);
            entity.setCustomScope(customScope);
            dataScopeMapper.insert(entity);
        } else {
            entity.setScopeType(scopeType);
            entity.setCustomScope(customScope);
            dataScopeMapper.updateById(entity);
        }

        log.info("Data scope set - roleCode={}, scopeType={}", roleCode, scopeType);
    }

    @Override
    public <T> LambdaQueryWrapper<T> filterByScope(LambdaQueryWrapper<T> queryWrapper,
                                                    Long userId,
                                                    Long departmentId,
                                                    Set<String> roleCodes) {
        // Find the most permissive scope among all roles
        String effectiveScope = determineEffectiveScope(roleCodes);

        Long tenantId = TenantContext.getTenantId();

        switch (effectiveScope) {
            case "ALL":
                // No filter applied
                break;

            case "SELF":
                queryWrapper.eq("create_user", userId);
                break;

            case "DEPARTMENT":
                if (departmentId != null) {
                    List<Long> deptIds = departmentMapper.selectChildDeptIds(departmentId,
                            tenantId != null ? tenantId : 1L);
                    if (!deptIds.isEmpty()) {
                        queryWrapper.in("department_id", deptIds);
                    } else {
                        queryWrapper.eq("department_id", departmentId);
                    }
                } else {
                    // Fallback to SELF if no department
                    queryWrapper.eq("create_user", userId);
                }
                break;

            case "DEPARTMENT_AND_SUB":
                if (departmentId != null) {
                    List<Long> allDeptIds = departmentMapper.selectAllDescendantDeptIds(departmentId,
                            tenantId != null ? tenantId : 1L);
                    if (!allDeptIds.isEmpty()) {
                        queryWrapper.in("department_id", allDeptIds);
                    } else {
                        queryWrapper.eq("department_id", departmentId);
                    }
                } else {
                    queryWrapper.eq("create_user", userId);
                }
                break;

            case "CUSTOM":
                String customScope = getCustomScopeForRoles(roleCodes);
                if (customScope != null) {
                    try {
                        List<Long> customDeptIds = objectMapper.readValue(customScope,
                                new TypeReference<List<Long>>() {});
                        if (!customDeptIds.isEmpty()) {
                            queryWrapper.in("department_id", customDeptIds);
                        } else {
                            queryWrapper.eq("create_user", userId);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse custom scope: {}", customScope, e);
                        queryWrapper.eq("create_user", userId);
                    }
                } else {
                    queryWrapper.eq("create_user", userId);
                }
                break;

            default:
                // Default to SELF
                queryWrapper.eq("create_user", userId);
                break;
        }

        return queryWrapper;
    }

    /**
     * Determine the most permissive effective scope from a set of role codes.
     * ALL > DEPARTMENT_AND_SUB > DEPARTMENT > CUSTOM > SELF
     */
    private String determineEffectiveScope(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return "SELF";
        }

        // SUPER_ADMIN always gets ALL
        if (roleCodes.contains("ROLE_SUPER_ADMIN")) {
            return "ALL";
        }

        String mostPermissive = "SELF";
        for (String roleCode : roleCodes) {
            String scope = getDataScope(roleCode);
            if (scopeIsMorePermissive(scope, mostPermissive)) {
                mostPermissive = scope;
            }
        }
        return mostPermissive;
    }

    private boolean scopeIsMorePermissive(String scope, String current) {
        int scopeRank = scopeRank(scope);
        int currentRank = scopeRank(current);
        return scopeRank > currentRank;
    }

    private int scopeRank(String scope) {
        return switch (scope) {
            case "SELF" -> 0;
            case "CUSTOM" -> 1;
            case "DEPARTMENT" -> 2;
            case "DEPARTMENT_AND_SUB" -> 3;
            case "ALL" -> 4;
            default -> 0;
        };
    }

    private String getCustomScopeForRoles(Set<String> roleCodes) {
        for (String roleCode : roleCodes) {
            DataScopeEntity entity = dataScopeMapper.selectOne(
                    new LambdaQueryWrapper<DataScopeEntity>()
                            .eq(DataScopeEntity::getRoleCode, roleCode)
                            .eq(DataScopeEntity::getIsDeleted, 0)
            );
            if (entity != null && "CUSTOM".equals(entity.getScopeType()) && entity.getCustomScope() != null) {
                return entity.getCustomScope();
            }
        }
        return null;
    }
}
