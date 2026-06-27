package com.zyy.tenant.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.tenant.model.vo.PlatformStatsVO;
import com.zyy.tenant.model.vo.TenantUsageVO;
import com.zyy.tenant.model.vo.TenantVO;
import com.zyy.tenant.service.SaaSAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for SaaS platform administration.
 * All endpoints require SUPER_ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping("/api/saas")
@RequiredArgsConstructor
@Tag(name = "SaaS Administration", description = "Platform-level SaaS management APIs (SUPER_ADMIN only)")
public class SaaSAdminController {

    private final SaaSAdminService saaSAdminService;

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List tenants with stats", description = "List all tenants with usage statistics")
    public Result<PageVO<TenantVO>> listTenants(
            @Valid PageParam pageParam,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        PageVO<TenantVO> page = saaSAdminService.listTenantsWithStats(
                pageParam.getPageNum(), pageParam.getPageSize(), keyword);
        return Result.ok(page);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Platform statistics", description = "Get platform-wide statistics including tenant counts, user counts, and plan distribution")
    public Result<PlatformStatsVO> getPlatformStats() {
        PlatformStatsVO stats = saaSAdminService.getPlatformStats();
        return Result.ok(stats);
    }

    @PostMapping("/tenants/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Suspend tenant", description = "Suspend a tenant, preventing all users from accessing the system")
    public Result<Void> suspendTenant(
            @Parameter(description = "Tenant ID") @PathVariable Long id) {
        saaSAdminService.suspendTenant(id);
        return Result.ok(null, "Tenant suspended successfully");
    }

    @PostMapping("/tenants/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate tenant", description = "Activate a previously suspended tenant")
    public Result<Void> activateTenant(
            @Parameter(description = "Tenant ID") @PathVariable Long id) {
        saaSAdminService.activateTenant(id);
        return Result.ok(null, "Tenant activated successfully");
    }

    @GetMapping("/tenants/{id}/usage")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Tenant usage stats", description = "Get tenant usage statistics including user count, asset count, and storage usage")
    public Result<TenantUsageVO> getTenantUsage(
            @Parameter(description = "Tenant ID") @PathVariable Long id) {
        TenantUsageVO usage = saaSAdminService.getTenantUsage(id);
        return Result.ok(usage);
    }
}
