package com.zyy.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * Data scope service for ABAC data permissions.
 * Provides methods to query and apply data scope rules based on role configuration.
 */
public interface DataScopeService {

    /**
     * Get the data scope type for a given role code.
     *
     * @param roleCode role code
     * @return scope type string (SELF/DEPARTMENT/DEPARTMENT_AND_SUB/ALL/CUSTOM)
     */
    String getDataScope(String roleCode);

    /**
     * Set or update data scope for a role.
     *
     * @param roleCode    role code
     * @param scopeType   scope type
     * @param customScope custom scope definition (JSON, nullable)
     */
    void setDataScope(String roleCode, String scopeType, String customScope);

    /**
     * Apply data scope filter to a query wrapper.
     * Automatically determines scope based on user's roles and applies the appropriate filter.
     *
     * @param <T>          entity type
     * @param queryWrapper the query wrapper to modify
     * @param userId       current user ID
     * @param departmentId current user's department ID
     * @param roleCodes    role codes of the current user
     * @return modified query wrapper with data scope applied
     */
    <T> LambdaQueryWrapper<T> filterByScope(LambdaQueryWrapper<T> queryWrapper,
                                             Long userId,
                                             Long departmentId,
                                             java.util.Set<String> roleCodes);
}
