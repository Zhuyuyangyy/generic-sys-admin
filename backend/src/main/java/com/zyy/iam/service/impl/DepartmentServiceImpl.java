package com.zyy.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.exception.BusinessException;
import com.zyy.iam.mapper.DepartmentMapper;
import com.zyy.iam.mapper.SysUserMapper;
import com.zyy.iam.model.dto.DepartmentSaveDTO;
import com.zyy.iam.model.dto.DepartmentUpdateDTO;
import com.zyy.iam.model.entity.DepartmentEntity;
import com.zyy.iam.model.entity.SysUserEntity;
import com.zyy.iam.model.vo.DepartmentVO;
import com.zyy.iam.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Department service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<DepartmentVO> getTree() {
        List<DepartmentEntity> allDepts = departmentMapper.selectList(
                new LambdaQueryWrapper<DepartmentEntity>()
                        .eq(DepartmentEntity::getIsDeleted, 0)
                        .orderByAsc(DepartmentEntity::getSort)
        );

        List<DepartmentVO> voList = allDepts.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return buildTree(voList, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO create(DepartmentSaveDTO saveDTO) {
        // Validate parent department exists
        if (saveDTO.getParentId() != null && saveDTO.getParentId() > 0) {
            DepartmentEntity parent = departmentMapper.selectById(saveDTO.getParentId());
            if (parent == null) {
                throw new BusinessException("Parent department not found: " + saveDTO.getParentId());
            }
        }

        DepartmentEntity entity = new DepartmentEntity();
        entity.setDeptName(saveDTO.getDeptName());
        entity.setParentId(saveDTO.getParentId() != null ? saveDTO.getParentId() : 0L);
        entity.setLeaderId(saveDTO.getLeaderId());
        entity.setSort(saveDTO.getSort() != null ? saveDTO.getSort() : 0);
        entity.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 1);

        departmentMapper.insert(entity);

        log.info("Department created - id={}, name={}", entity.getId(), entity.getDeptName());
        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO update(DepartmentUpdateDTO updateDTO) {
        DepartmentEntity entity = departmentMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Department not found: " + updateDTO.getId());
        }

        if (updateDTO.getDeptName() != null) {
            entity.setDeptName(updateDTO.getDeptName());
        }
        if (updateDTO.getParentId() != null) {
            // Prevent circular reference
            if (updateDTO.getParentId().equals(entity.getId())) {
                throw new BusinessException("Department cannot be its own parent");
            }
            entity.setParentId(updateDTO.getParentId());
        }
        if (updateDTO.getLeaderId() != null) {
            entity.setLeaderId(updateDTO.getLeaderId());
        }
        if (updateDTO.getSort() != null) {
            entity.setSort(updateDTO.getSort());
        }
        if (updateDTO.getStatus() != null) {
            entity.setStatus(updateDTO.getStatus());
        }

        departmentMapper.updateById(entity);

        log.info("Department updated - id={}", entity.getId());
        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Department not found: " + id);
        }

        // Check for children
        long childCount = departmentMapper.selectCount(
                new LambdaQueryWrapper<DepartmentEntity>()
                        .eq(DepartmentEntity::getParentId, id)
                        .eq(DepartmentEntity::getIsDeleted, 0)
        );
        if (childCount > 0) {
            throw new BusinessException("Cannot delete department with child departments");
        }

        departmentMapper.deleteById(id);
        log.info("Department deleted - id={}", id);
    }

    @Override
    public List<DepartmentVO> getTreeForSelector() {
        return getTree();
    }

    private DepartmentVO entityToVO(DepartmentEntity entity) {
        String leaderName = null;
        if (entity.getLeaderId() != null) {
            SysUserEntity leader = sysUserMapper.selectById(entity.getLeaderId());
            if (leader != null) {
                leaderName = leader.getRealName();
            }
        }

        String statusText = entity.getStatus() == 1 ? "Active" : "Disabled";

        return DepartmentVO.builder()
                .id(entity.getId())
                .deptName(entity.getDeptName())
                .parentId(entity.getParentId())
                .leaderId(entity.getLeaderId())
                .leaderName(leaderName)
                .sort(entity.getSort())
                .status(entity.getStatus())
                .statusText(statusText)
                .createTime(entity.getCreateTime())
                .build();
    }

    private List<DepartmentVO> buildTree(List<DepartmentVO> depts, Long parentId) {
        return depts.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .peek(d -> {
                    List<DepartmentVO> children = buildTree(depts, d.getId());
                    if (!children.isEmpty()) {
                        d.setChildren(children);
                    }
                })
                .collect(Collectors.toList());
    }
}
