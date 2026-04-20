package com.zyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.mapper.SysMenuMapper;
import com.zyy.mapper.SysRoleMapper;
import com.zyy.mapper.SysRoleMenuMapper;
import com.zyy.mapper.SysUserRoleMapper;
import com.zyy.model.entity.SysRoleEntity;
import com.zyy.model.vo.PageVO;
import com.zyy.model.vo.SysRoleVO;
import com.zyy.service.SysMenuService;
import com.zyy.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysMenuService menuService;

    @Override
    public PageVO<SysRoleVO> getPage(Long pageNum, Long pageSize, String name, Integer status) {
        Page<SysRoleEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleEntity::getIsDeleted, 0);
        if (name != null && !name.isBlank()) {
            wrapper.like(SysRoleEntity::getName, name);
        }
        if (status != null) {
            wrapper.eq(SysRoleEntity::getStatus, status);
        }
        wrapper.orderByDesc(SysRoleEntity::getSortOrder);

        Page<SysRoleEntity> result = roleMapper.selectPage(page, wrapper);
        List<SysRoleVO> voList = result.getRecords().stream().map(this::entityToVO).collect(Collectors.toList());

        return PageVO.<SysRoleVO>builder()
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
    public SysRoleVO getById(Long id) {
        SysRoleEntity entity = roleMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() == 1) {
            throw new BusinessException("角色不存在");
        }
        SysRoleVO vo = entityToVO(entity);
        // 加载菜单ID列表
        vo.setMenuIds(menuService.getMenuIdsByRoleId(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO save(SysRoleEntity entity, Long operatorId) {
        // 检查编码唯一
        long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getCode, entity.getCode())
                .eq(SysRoleEntity::getIsDeleted, 0));
        if (count > 0) {
            throw new BusinessException("角色编码已存在: " + entity.getCode());
        }
        entity.setIsDeleted(0);
        entity.setCreateUser(operatorId);
        roleMapper.insert(entity);
        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO update(SysRoleEntity entity, Long operatorId) {
        SysRoleEntity existing = roleMapper.selectById(entity.getId());
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BusinessException("角色不存在");
        }
        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        existing.setStatus(entity.getStatus());
        existing.setSortOrder(entity.getSortOrder());
        roleMapper.updateById(existing);
        return entityToVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        // 检查是否有关联用户
        List<Long> userIds = userRoleMapper.selectRoleIdsByUserId(id);
        if (!userIds.isEmpty()) {
            throw new BusinessException("该角色已分配给用户，无法删除");
        }
        // 软删除
        SysRoleEntity entity = roleMapper.selectById(id);
        entity.setIsDeleted(1);
        roleMapper.updateById(entity);
        log.info("角色已删除 - roleId={}, operatorId={}", id, operatorId);
    }

    @Override
    public List<SysRoleVO> getAllEnabled() {
        List<SysRoleEntity> list = roleMapper.selectList(
                new LambdaQueryWrapper<SysRoleEntity>()
                        .eq(SysRoleEntity::getIsDeleted, 0)
                        .eq(SysRoleEntity::getStatus, 1)
                        .orderByAsc(SysRoleEntity::getSortOrder)
        );
        return list.stream().map(this::entityToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMenus(Long roleId, List<Long> menuIds) {
        menuService.grantMenus(roleId, menuIds);
    }

    private SysRoleVO entityToVO(SysRoleEntity entity) {
        return SysRoleVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .sortOrder(entity.getSortOrder())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
