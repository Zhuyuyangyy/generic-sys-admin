package com.zyy.tenant.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.security.LoginUser;
import com.zyy.tenant.model.dto.TenantRegisterDTO;
import com.zyy.tenant.model.dto.TenantUpdateDTO;
import com.zyy.tenant.model.vo.TenantVO;
import com.zyy.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RESTful controller for tenant management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Multi-tenant management APIs")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register new tenant", description = "Create a new tenant (super admin only)")
    public Result<TenantVO> register(@Valid @RequestBody TenantRegisterDTO registerDTO) {
        TenantVO tenant = tenantService.register(registerDTO);
        return Result.ok(tenant, "Tenant registered successfully");
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List tenants", description = "Retrieve paginated list of tenants (super admin only)")
    public Result<PageVO<TenantVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        PageVO<TenantVO> page = tenantService.getPage(pageParam.getPageNum(), pageParam.getPageSize(), keyword);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Get tenant detail", description = "Retrieve tenant details by ID")
    public Result<TenantVO> getById(
            @Parameter(description = "Tenant ID") @PathVariable Long id) {
        TenantVO tenant = tenantService.getById(id);
        return Result.ok(tenant);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update tenant", description = "Update tenant information")
    public Result<TenantVO> update(
            @Parameter(description = "Tenant ID") @PathVariable Long id,
            @Valid @RequestBody TenantUpdateDTO updateDTO) {
        updateDTO.setId(id);
        TenantVO tenant = tenantService.update(updateDTO);
        return Result.ok(tenant, "Tenant updated successfully");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update tenant status", description = "Suspend, activate, or terminate a tenant")
    public Result<Void> updateStatus(
            @Parameter(description = "Tenant ID") @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.fail(400, "Status is required");
        }
        tenantService.updateStatus(id, status);
        return Result.ok(null, "Tenant status updated successfully");
    }

    @GetMapping("/current")
    @Operation(summary = "Get current tenant", description = "Retrieve the current user's tenant information")
    public Result<TenantVO> getCurrentTenant() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        TenantVO tenant = tenantService.getCurrentTenant(loginUser.getTenantId());
        return Result.ok(tenant);
    }
}
