package com.zyy.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.common.PageVO;
import com.zyy.exception.BusinessException;
import com.zyy.iam.mapper.SysUserMapper;
import com.zyy.iam.model.entity.SysUserEntity;
import com.zyy.tenant.mapper.TenantMapper;
import com.zyy.tenant.model.entity.TenantEntity;
import com.zyy.tenant.model.vo.PlatformStatsVO;
import com.zyy.tenant.model.vo.TenantUsageVO;
import com.zyy.tenant.model.vo.TenantVO;
import com.zyy.tenant.service.SaaSAdminService;
import com.zyy.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SaaS platform administration service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaaSAdminServiceImpl implements SaaSAdminService {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;
    private final TenantService tenantService;

    @Override
    public PageVO<TenantVO> listTenantsWithStats(Long pageNum, Long pageSize, String keyword) {
        return tenantService.getPage(pageNum, pageSize, keyword);
    }

    @Override
    public PlatformStatsVO getPlatformStats() {
        Long totalTenants = tenantMapper.selectCount(
                new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getIsDeleted, 0)
        );

        Long activeTenants = tenantMapper.selectCount(
                new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getIsDeleted, 0)
                        .eq(TenantEntity::getStatus, 1)
        );

        Long totalUsers = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getIsDeleted, 0)
        );

        // Count by subscription plan
        List<TenantEntity> allTenants = tenantMapper.selectList(
                new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getIsDeleted, 0)
        );
        Map<String, Long> byPlan = allTenants.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getSubscriptionPlan() != null ? t.getSubscriptionPlan() : "FREE",
                        Collectors.counting()
                ));

        return PlatformStatsVO.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .totalUsers(totalUsers)
                .totalAssets(0L) // TODO: count from equipment table
                .revenue(0.0) // TODO: integrate with billing system
                .byPlan(byPlan)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendTenant(Long tenantId) {
        tenantService.updateStatus(tenantId, 0); // SUSPENDED
        log.info("Tenant suspended - id={}", tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateTenant(Long tenantId) {
        tenantService.updateStatus(tenantId, 1); // ACTIVE
        log.info("Tenant activated - id={}", tenantId);
    }

    @Override
    public TenantUsageVO getTenantUsage(Long tenantId) {
        TenantEntity tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("Tenant not found: " + tenantId);
        }

        Long userCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getTenantId, tenantId)
                        .eq(SysUserEntity::getIsDeleted, 0)
        );

        int maxUsers = tenant.getMaxUsers() != null ? tenant.getMaxUsers() : 0;
        int maxAssets = tenant.getMaxAssets() != null ? tenant.getMaxAssets() : 0;

        double userUsagePercentage = maxUsers > 0 ? (userCount.doubleValue() / maxUsers) * 100 : 0.0;

        return TenantUsageVO.builder()
                .tenantId(tenantId)
                .tenantName(tenant.getTenantName())
                .userCount(userCount)
                .maxUsers(maxUsers)
                .userUsagePercentage(Math.round(userUsagePercentage * 100.0) / 100.0)
                .assetCount(0L) // TODO: count from equipment table
                .maxAssets(maxAssets)
                .assetUsagePercentage(0.0)
                .storageUsed(0L) // TODO: track storage usage
                .storageLimit(0L) // TODO: configure storage limits
                .storageUsagePercentage(0.0)
                .build();
    }

    @Override
    public void checkUserLimit(Long tenantId) {
        TenantEntity tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("Tenant not found: " + tenantId);
        }

        if (tenant.getStatus() != 1) {
            throw new BusinessException("Tenant is not active");
        }

        Long userCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getTenantId, tenantId)
                        .eq(SysUserEntity::getIsDeleted, 0)
        );

        int maxUsers = tenant.getMaxUsers() != null ? tenant.getMaxUsers() : 0;
        if (maxUsers > 0 && userCount >= maxUsers) {
            throw new BusinessException("User limit exceeded for tenant. Current: " + userCount + ", Max: " + maxUsers);
        }
    }

    @Override
    public void checkAssetLimit(Long tenantId) {
        TenantEntity tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("Tenant not found: " + tenantId);
        }

        if (tenant.getStatus() != 1) {
            throw new BusinessException("Tenant is not active");
        }

        // TODO: implement asset count check when equipment mapper is available
        // For now, just check maxAssets config
        int maxAssets = tenant.getMaxAssets() != null ? tenant.getMaxAssets() : 0;
        if (maxAssets > 0) {
            // Placeholder for actual asset count
            long assetCount = 0L;
            if (assetCount >= maxAssets) {
                throw new BusinessException("Asset limit exceeded for tenant. Current: " + assetCount + ", Max: " + maxAssets);
            }
        }
    }

    @Override
    public void checkStorageLimit(Long tenantId, long fileSizeBytes) {
        TenantEntity tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("Tenant not found: " + tenantId);
        }

        if (tenant.getStatus() != 1) {
            throw new BusinessException("Tenant is not active");
        }

        // TODO: implement storage tracking
        // For now, no storage limit enforcement
        log.debug("Storage limit check for tenant {} - file size: {} bytes", tenantId, fileSizeBytes);
    }
}
