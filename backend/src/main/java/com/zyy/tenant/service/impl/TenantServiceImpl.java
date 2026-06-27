package com.zyy.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.common.PageVO;
import com.zyy.exception.BusinessException;
import com.zyy.iam.mapper.SysUserMapper;
import com.zyy.tenant.mapper.TenantMapper;
import com.zyy.tenant.model.dto.TenantRegisterDTO;
import com.zyy.tenant.model.dto.TenantUpdateDTO;
import com.zyy.tenant.model.entity.TenantEntity;
import com.zyy.tenant.model.vo.TenantStatsVO;
import com.zyy.tenant.model.vo.TenantVO;
import com.zyy.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tenant management service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantVO register(TenantRegisterDTO registerDTO) {
        // Check tenant code uniqueness
        long existingCount = tenantMapper.selectCount(
                new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getTenantCode, registerDTO.getTenantCode())
                        .eq(TenantEntity::getIsDeleted, 0)
        );
        if (existingCount > 0) {
            throw new BusinessException("Tenant code already exists: " + registerDTO.getTenantCode());
        }

        TenantEntity entity = new TenantEntity();
        entity.setTenantCode(registerDTO.getTenantCode());
        entity.setTenantName(registerDTO.getTenantName());
        entity.setContactPerson(registerDTO.getContactPerson());
        entity.setContactEmail(registerDTO.getContactEmail());
        entity.setContactPhone(registerDTO.getContactPhone());
        entity.setDomain(registerDTO.getDomain());
        entity.setLogoUrl(registerDTO.getLogoUrl());
        entity.setSubscriptionPlan(registerDTO.getSubscriptionPlan() != null
                ? registerDTO.getSubscriptionPlan() : "FREE");
        entity.setMaxUsers(registerDTO.getMaxUsers() != null ? registerDTO.getMaxUsers() : getDefaultMaxUsers(entity.getSubscriptionPlan()));
        entity.setMaxAssets(registerDTO.getMaxAssets() != null ? registerDTO.getMaxAssets() : getDefaultMaxAssets(entity.getSubscriptionPlan()));
        entity.setStatus(1); // ACTIVE

        tenantMapper.insert(entity);

        log.info("Tenant registered - id={}, code={}", entity.getId(), entity.getTenantCode());
        return entityToVO(entity);
    }

    @Override
    public PageVO<TenantVO> getPage(Long pageNum, Long pageSize, String keyword) {
        Page<TenantEntity> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<TenantEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantEntity::getIsDeleted, 0);

        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(w -> w
                    .like(TenantEntity::getTenantCode, keyword)
                    .or().like(TenantEntity::getTenantName, keyword)
                    .or().like(TenantEntity::getContactPerson, keyword)
            );
        }
        queryWrapper.orderByDesc(TenantEntity::getCreateTime);

        Page<TenantEntity> result = tenantMapper.selectPage(page, queryWrapper);

        List<TenantVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<TenantVO>builder()
                .items(voList)
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .isFirst(result.getCurrent() == 1)
                .isLast(result.getCurrent() >= result.getPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
    }

    @Override
    public TenantVO getById(Long id) {
        TenantEntity entity = tenantMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Tenant not found: " + id);
        }
        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantVO update(TenantUpdateDTO updateDTO) {
        TenantEntity entity = tenantMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Tenant not found: " + updateDTO.getId());
        }

        if (updateDTO.getTenantName() != null) {
            entity.setTenantName(updateDTO.getTenantName());
        }
        if (updateDTO.getContactPerson() != null) {
            entity.setContactPerson(updateDTO.getContactPerson());
        }
        if (updateDTO.getContactEmail() != null) {
            entity.setContactEmail(updateDTO.getContactEmail());
        }
        if (updateDTO.getContactPhone() != null) {
            entity.setContactPhone(updateDTO.getContactPhone());
        }
        if (updateDTO.getDomain() != null) {
            entity.setDomain(updateDTO.getDomain());
        }
        if (updateDTO.getLogoUrl() != null) {
            entity.setLogoUrl(updateDTO.getLogoUrl());
        }
        if (updateDTO.getSubscriptionPlan() != null) {
            entity.setSubscriptionPlan(updateDTO.getSubscriptionPlan());
        }
        if (updateDTO.getMaxUsers() != null) {
            entity.setMaxUsers(updateDTO.getMaxUsers());
        }
        if (updateDTO.getMaxAssets() != null) {
            entity.setMaxAssets(updateDTO.getMaxAssets());
        }

        tenantMapper.updateById(entity);

        log.info("Tenant updated - id={}", entity.getId());
        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        TenantEntity entity = tenantMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Tenant not found: " + id);
        }

        entity.setStatus(status);
        tenantMapper.updateById(entity);

        String statusText = switch (status) {
            case 1 -> "ACTIVE";
            case 0 -> "SUSPENDED";
            case 2 -> "TERMINATED";
            default -> "UNKNOWN";
        };
        log.info("Tenant status updated - id={}, status={}", id, statusText);
    }

    @Override
    public TenantVO getCurrentTenant(Long tenantId) {
        TenantEntity entity = tenantMapper.selectById(tenantId);
        if (entity == null) {
            throw new BusinessException("Tenant not found: " + tenantId);
        }
        return entityToVO(entity);
    }

    @Override
    public TenantStatsVO getStats(Long tenantId) {
        TenantEntity entity = tenantMapper.selectById(tenantId);
        if (entity == null) {
            throw new BusinessException("Tenant not found: " + tenantId);
        }

        // Count users in this tenant
        Long userCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<com.zyy.iam.model.entity.SysUserEntity>()
                        .eq(com.zyy.iam.model.entity.SysUserEntity::getTenantId, tenantId)
                        .eq(com.zyy.iam.model.entity.SysUserEntity::getIsDeleted, 0)
        );

        return TenantStatsVO.builder()
                .tenantId(tenantId)
                .tenantName(entity.getTenantName())
                .userCount(userCount)
                .assetCount(0L) // TODO: count assets when equipment table query is available
                .storageUsed(0L) // TODO: track storage usage
                .subscriptionPlan(entity.getSubscriptionPlan())
                .maxUsers(entity.getMaxUsers())
                .maxAssets(entity.getMaxAssets())
                .build();
    }

    private TenantVO entityToVO(TenantEntity entity) {
        String statusText = switch (entity.getStatus()) {
            case 1 -> "ACTIVE";
            case 0 -> "SUSPENDED";
            case 2 -> "TERMINATED";
            default -> "UNKNOWN";
        };

        return TenantVO.builder()
                .id(entity.getId())
                .tenantCode(entity.getTenantCode())
                .tenantName(entity.getTenantName())
                .contactPerson(entity.getContactPerson())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .domain(entity.getDomain())
                .logoUrl(entity.getLogoUrl())
                .subscriptionPlan(entity.getSubscriptionPlan())
                .maxUsers(entity.getMaxUsers())
                .maxAssets(entity.getMaxAssets())
                .status(entity.getStatus())
                .statusText(statusText)
                .expiryDate(entity.getExpiryDate())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private int getDefaultMaxUsers(String plan) {
        return switch (plan) {
            case "FREE" -> 5;
            case "BASIC" -> 20;
            case "PROFESSIONAL" -> 100;
            case "ENTERPRISE" -> 1000;
            default -> 5;
        };
    }

    private int getDefaultMaxAssets(String plan) {
        return switch (plan) {
            case "FREE" -> 100;
            case "BASIC" -> 500;
            case "PROFESSIONAL" -> 5000;
            case "ENTERPRISE" -> 10000;
            default -> 100;
        };
    }
}
