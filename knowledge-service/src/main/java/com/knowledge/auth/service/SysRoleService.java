package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.*;
import com.knowledge.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleRegionMapper roleRegionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<SysRole> listAll() {
        return roleMapper.selectList(null);
    }

    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    public void save(SysRole role) {
        roleMapper.insert(role);
    }

    public void update(SysRole role) {
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, id));
        roleRegionMapper.delete(new LambdaQueryWrapper<SysRoleRegion>().eq(SysRoleRegion::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    @Transactional
    public void assignDepts(Long roleId, List<Long> deptIds) {
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        for (Long deptId : deptIds) {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(roleId);
            rd.setDeptId(deptId);
            roleDeptMapper.insert(rd);
        }
    }

    @Transactional
    public void assignRegions(Long roleId, List<Long> regionIds) {
        roleRegionMapper.delete(new LambdaQueryWrapper<SysRoleRegion>().eq(SysRoleRegion::getRoleId, roleId));
        for (Long regionId : regionIds) {
            SysRoleRegion rr = new SysRoleRegion();
            rr.setRoleId(roleId);
            rr.setRegionId(regionId);
            roleRegionMapper.insert(rr);
        }
    }

    public List<Long> getAssignedMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    public List<Long> getAssignedDeptIds(Long roleId) {
        return roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId))
                .stream().map(SysRoleDept::getDeptId).toList();
    }

    public List<Long> getAssignedRegionIds(Long roleId) {
        return roleRegionMapper.selectList(
                new LambdaQueryWrapper<SysRoleRegion>().eq(SysRoleRegion::getRoleId, roleId))
                .stream().map(SysRoleRegion::getRegionId).toList();
    }
}
