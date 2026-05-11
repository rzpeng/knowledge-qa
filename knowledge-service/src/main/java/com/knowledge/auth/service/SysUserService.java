package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.auth.entity.*;
import com.knowledge.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserService {
    private final SysUserMapper userMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRegionMapper userRegionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public Page<SysUser> page(int page, int size) {
        return userMapper.selectPage(new Page<>(page, size), null);
    }

    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    public void save(SysUser user) {
        userMapper.insert(user);
    }

    public void update(SysUser user) {
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, id));
        userRegionMapper.delete(new LambdaQueryWrapper<SysUserRegion>().eq(SysUserRegion::getUserId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userMapper.deleteById(id);
    }

    @Transactional
    public void assignDepts(Long userId, List<Long> deptIds, List<Long> leaderDeptIds) {
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId));
        if (deptIds != null) {
            for (Long deptId : deptIds) {
                SysUserDept ud = new SysUserDept();
                ud.setUserId(userId);
                ud.setDeptId(deptId);
                ud.setIsLeader(leaderDeptIds != null && leaderDeptIds.contains(deptId) ? 1 : 0);
                userDeptMapper.insert(ud);
            }
        }
    }

    @Transactional
    public void assignRegions(Long userId, List<Long> regionIds) {
        userRegionMapper.delete(new LambdaQueryWrapper<SysUserRegion>().eq(SysUserRegion::getUserId, userId));
        if (regionIds != null) {
            for (Long regionId : regionIds) {
                SysUserRegion ur = new SysUserRegion();
                ur.setUserId(userId);
                ur.setRegionId(regionId);
                userRegionMapper.insert(ur);
            }
        }
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    public List<Long> getDeptIds(Long userId) {
        return userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId))
                .stream().map(SysUserDept::getDeptId).toList();
    }

    public List<Long> getRegionIds(Long userId) {
        return userRegionMapper.selectList(
                new LambdaQueryWrapper<SysUserRegion>().eq(SysUserRegion::getUserId, userId))
                .stream().map(SysUserRegion::getRegionId).toList();
    }

    public List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }
}
