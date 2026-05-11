package com.knowledge.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.*;
import com.knowledge.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysAccountMapper accountMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRegionMapper userRegionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<SysAccount>()
                        .eq(SysAccount::getUsername, username)
                        .eq(SysAccount::getStatus, 1)
        );
        if (account == null) {
            throw new UsernameNotFoundException("账号不存在或已禁用");
        }

        SysUser user = userMapper.selectById(account.getUserId());
        if (user == null || user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }

        // Get user departments
        List<Long> deptIds = userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, user.getId())
        ).stream().map(SysUserDept::getDeptId).collect(Collectors.toList());

        // Get user regions
        List<Long> regionIds = userRegionMapper.selectList(
                new LambdaQueryWrapper<SysUserRegion>().eq(SysUserRegion::getUserId, user.getId())
        ).stream().map(SysUserRegion::getRegionId).collect(Collectors.toList());

        // Get permissions
        List<String> permissions = new ArrayList<>();
        if (account.getIsSuperAdmin() != 1) {
            List<SysUserRole> userRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId())
            );
            if (!userRoles.isEmpty()) {
                List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
                List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds)
                );
                if (!roleMenus.isEmpty()) {
                    List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
                    List<SysMenu> menus = menuMapper.selectList(
                            new LambdaQueryWrapper<SysMenu>()
                                    .in(SysMenu::getId, menuIds)
                                    .isNotNull(SysMenu::getPermission)
                    );
                    permissions = menus.stream().map(SysMenu::getPermission)
                            .filter(p -> p != null && !p.isEmpty())
                            .collect(Collectors.toList());
                }
            }
        }

        return LoginUserDetails.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .username(account.getUsername())
                .password(account.getPassword())
                .userName(user.getName())
                .isSuperAdmin(account.getIsSuperAdmin())
                .permissions(permissions)
                .deptIds(deptIds)
                .regionIds(regionIds)
                .build();
    }
}
