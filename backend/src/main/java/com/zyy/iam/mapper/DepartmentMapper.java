package com.zyy.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.iam.model.entity.DepartmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus Mapper interface for sys_department persistence operations.
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentEntity> {

    /**
     * Find all child department IDs (including the specified department itself).
     *
     * @param parentId parent department ID
     * @param tenantId tenant ID
     * @return list of department IDs
     */
    @Select("SELECT id FROM sys_department WHERE (id = #{parentId} OR parent_id = #{parentId}) AND tenant_id = #{tenantId} AND is_deleted = 0 AND status = 1")
    List<Long> selectChildDeptIds(@Param("parentId") Long parentId, @Param("tenantId") Long tenantId);

    /**
     * Find all descendant department IDs recursively.
     *
     * @param parentId parent department ID
     * @param tenantId tenant ID
     * @return list of all descendant department IDs
     */
    @Select("WITH RECURSIVE dept_tree AS (" +
            "  SELECT id FROM sys_department WHERE id = #{parentId} AND tenant_id = #{tenantId} AND is_deleted = 0 AND status = 1 " +
            "  UNION ALL " +
            "  SELECT d.id FROM sys_department d INNER JOIN dept_tree dt ON d.parent_id = dt.id WHERE d.tenant_id = #{tenantId} AND d.is_deleted = 0 AND d.status = 1" +
            ") SELECT id FROM dept_tree")
    List<Long> selectAllDescendantDeptIds(@Param("parentId") Long parentId, @Param("tenantId") Long tenantId);
}
