package com.knowledge.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.auth.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT m.* FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} ORDER BY m.sort_order")
    List<SysMenu> selectMenuListByRoleId(Long roleId);
}
