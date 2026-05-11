package com.knowledge.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.auth.entity.SysUser;
import com.knowledge.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT r.dept_data_scope, r.region_data_scope " +
            "FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId}")
    List<SysRole> selectRoleDataScopesByUserId(Long userId);

    @Select("SELECT rd.dept_id FROM sys_role_dept rd " +
            "JOIN sys_user_role ur ON rd.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Long> selectCustomDeptIdsByUserId(Long userId);

    @Select("SELECT rr.region_id FROM sys_role_region rr " +
            "JOIN sys_user_role ur ON rr.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Long> selectCustomRegionIdsByUserId(Long userId);
}
