package com.zyy.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.iam.model.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus Mapper interface for sys_role persistence operations.
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {

    /**
     * Select role codes by user ID through user-role association.
     *
     * @param userId User ID
     * @return List of role codes
     */
    @Select("SELECT r.code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.is_deleted = 0")
    List<String> selectCodeByUserId(@Param("userId") Long userId);
}
