package com.zyy.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.zyy.iam.mapper.DataScopeMapper;
import com.zyy.iam.mapper.DepartmentMapper;
import com.zyy.iam.model.entity.DataScopeEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Data scope filter that automatically applies ABAC data permission filtering
 * to MyBatis-Plus queries.
 *
 * This interceptor inspects the current user's roles and applies the appropriate
 * data scope filter based on the configured scope type:
 * - SELF: WHERE created_by = currentUserId
 * - DEPARTMENT: WHERE department_id IN (user's department)
 * - DEPARTMENT_AND_SUB: WHERE department_id IN (user's department + sub-departments)
 * - ALL: No filter
 * - CUSTOM: WHERE department_id IN (custom department list)
 */
@Slf4j
@Component
public class DataScopeFilter implements DataPermissionHandler {

    private final DataScopeMapper dataScopeMapper;
    private final DepartmentMapper departmentMapper;
    private final ObjectMapper objectMapper;

    /** Tables excluded from data scope filtering */
    private static final Set<String> EXCLUDE_TABLES = Set.of(
            "sys_tenant", "sys_department", "data_scope",
            "sys_role", "sys_menu", "sys_user_role", "sys_role_menu",
            "token_blacklist", "rate_limit_log"
    );

    public DataScopeFilter(DataScopeMapper dataScopeMapper,
                           DepartmentMapper departmentMapper,
                           ObjectMapper objectMapper) {
        this.dataScopeMapper = dataScopeMapper;
        this.departmentMapper = departmentMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // Skip excluded tables and system tables
        if (shouldSkip(mappedStatementId)) {
            return where;
        }

        LoginUser loginUser = getCurrentLoginUser();
        if (loginUser == null) {
            return where;
        }

        // SUPER_ADMIN bypasses data scope filtering
        boolean isSuperAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) {
            return where;
        }

        // Determine the effective scope from the user's roles
        String effectiveScope = determineEffectiveScope(loginUser);
        Expression scopeExpression = buildScopeExpression(effectiveScope, loginUser);

        if (scopeExpression == null) {
            return where;
        }

        return where == null ? scopeExpression :
                new net.sf.jsqlparser.expression.operators.conditional.AndExpression(where, scopeExpression);
    }

    private boolean shouldSkip(String mappedStatementId) {
        if (mappedStatementId == null) {
            return true;
        }
        // Skip if the mapped statement is from excluded mappers
        return EXCLUDE_TABLES.stream()
                .anyMatch(table -> mappedStatementId.contains(table));
    }

    private LoginUser getCurrentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    private String determineEffectiveScope(LoginUser loginUser) {
        Set<String> roleCodes = new HashSet<>();
        loginUser.getAuthorities().forEach(a -> roleCodes.add(a.getAuthority()));

        if (roleCodes.contains("ROLE_SUPER_ADMIN")) {
            return "ALL";
        }

        String mostPermissive = "SELF";
        for (String roleCode : roleCodes) {
            String cleanCode = roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode;
            DataScopeEntity entity = dataScopeMapper.selectOne(
                    new LambdaQueryWrapper<DataScopeEntity>()
                            .eq(DataScopeEntity::getRoleCode, cleanCode)
                            .eq(DataScopeEntity::getIsDeleted, 0)
            );
            String scope = entity != null ? entity.getScopeType() : "SELF";
            if (scopeRank(scope) > scopeRank(mostPermissive)) {
                mostPermissive = scope;
            }
        }
        return mostPermissive;
    }

    private Expression buildScopeExpression(String scopeType, LoginUser loginUser) {
        Long userId = loginUser.getUserId();
        Long departmentId = loginUser.getDepartmentId();
        Long tenantId = loginUser.getTenantId();

        try {
            return switch (scopeType) {
                case "ALL" -> null; // No filter
                case "SELF" -> new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                        new Column("create_user"),
                        new net.sf.jsqlparser.expression.LongValue(userId));
                case "DEPARTMENT" -> buildDepartmentExpression(departmentId, tenantId, false);
                case "DEPARTMENT_AND_SUB" -> buildDepartmentExpression(departmentId, tenantId, true);
                case "CUSTOM" -> buildCustomScopeExpression(loginUser);
                default -> new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                        new Column("create_user"),
                        new net.sf.jsqlparser.expression.LongValue(userId));
            };
        } catch (Exception e) {
            log.warn("Failed to build data scope expression, falling back to SELF", e);
            return new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                    new Column("create_user"),
                    new net.sf.jsqlparser.expression.LongValue(userId));
        }
    }

    private Expression buildDepartmentExpression(Long departmentId, Long tenantId, boolean includeSubDepts) {
        if (departmentId == null) {
            return new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                    new Column("create_user"),
                    new net.sf.jsqlparser.expression.LongValue(
                            ((LoginUser) SecurityContextHolder.getContext()
                                    .getAuthentication().getPrincipal()).getUserId()));
        }

        List<Long> deptIds;
        if (includeSubDepts) {
            deptIds = departmentMapper.selectAllDescendantDeptIds(departmentId,
                    tenantId != null ? tenantId : 1L);
        } else {
            deptIds = departmentMapper.selectChildDeptIds(departmentId,
                    tenantId != null ? tenantId : 1L);
        }

        if (deptIds == null || deptIds.isEmpty()) {
            deptIds = List.of(departmentId);
        }

        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(new Column("department_id"));
        ExpressionList expressionList = new ExpressionList();
        List<Expression> expressions = new ArrayList<>();
        for (Long deptId : deptIds) {
            expressions.add(new net.sf.jsqlparser.expression.LongValue(deptId));
        }
        expressionList.setExpressions(expressions);
        inExpression.setRightExpression((Expression) expressionList);

        return inExpression;
    }

    private Expression buildCustomScopeExpression(LoginUser loginUser) {
        Set<String> roleCodes = new HashSet<>();
        loginUser.getAuthorities().forEach(a -> roleCodes.add(a.getAuthority()));

        for (String roleCode : roleCodes) {
            String cleanCode = roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode;
            DataScopeEntity entity = dataScopeMapper.selectOne(
                    new LambdaQueryWrapper<DataScopeEntity>()
                            .eq(DataScopeEntity::getRoleCode, cleanCode)
                            .eq(DataScopeEntity::getIsDeleted, 0)
            );
            if (entity != null && "CUSTOM".equals(entity.getScopeType()) && entity.getCustomScope() != null) {
                try {
                    List<Long> customDeptIds = objectMapper.readValue(entity.getCustomScope(),
                            new TypeReference<List<Long>>() {});
                    if (!customDeptIds.isEmpty()) {
                        InExpression inExpression = new InExpression();
                        inExpression.setLeftExpression(new Column("department_id"));
                        ExpressionList expressionList = new ExpressionList();
                        List<Expression> expressions = new ArrayList<>();
                        for (Long deptId : customDeptIds) {
                            expressions.add(new net.sf.jsqlparser.expression.LongValue(deptId));
                        }
                        expressionList.setExpressions(expressions);
                        inExpression.setRightExpression((Expression) expressionList);
                        return inExpression;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse custom scope", e);
                }
            }
        }

        // Fallback to SELF
        return new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                new Column("create_user"),
                new net.sf.jsqlparser.expression.LongValue(loginUser.getUserId()));
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
}
