package com.knowledge.auth.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.*;
import com.knowledge.auth.enums.DataScopeDeptEnum;
import com.knowledge.auth.enums.DataScopeRegionEnum;
import com.knowledge.auth.mapper.*;
import com.knowledge.auth.security.LoginUserDetails;
import com.knowledge.common.annotation.DataPermission;
import com.knowledge.common.annotation.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements DataScopeService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleRegionMapper roleRegionMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRegionMapper userRegionMapper;

    @Override
    public String buildDataScopeSql(DataPermission annotation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUserDetails user)) {
            return "";
        }

        // Super admin: no filtering
        if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1) {
            return "";
        }

        String alias = annotation.tableAlias();
        String prefix = StringUtils.hasText(alias) ? alias + "." : "";
        List<String> conditions = new ArrayList<>();

        // Department data permission
        if (annotation.enableDept()) {
            String deptCondition = buildDeptCondition(user, annotation, prefix);
            if (StringUtils.hasText(deptCondition)) {
                conditions.add(deptCondition);
            }
        }

        // Region data permission
        if (annotation.enableRegion()) {
            String regionCondition = buildRegionCondition(user, annotation, prefix);
            if (StringUtils.hasText(regionCondition)) {
                conditions.add(regionCondition);
            }
        }

        if (conditions.isEmpty()) {
            return "";
        }
        return String.join(" AND ", conditions);
    }

    private String buildDeptCondition(LoginUserDetails user, DataPermission annotation, String prefix) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId())
        );
        if (userRoles.isEmpty()) return "";

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);

        // If any role has "ALL" scope, no department filtering
        boolean hasAllDept = roles.stream().anyMatch(r -> r.getDeptDataScope() == DataScopeDeptEnum.ALL.getValue());
        if (hasAllDept) return "";

        // Take the most restrictive scope (highest number = smallest scope)
        int minScope = roles.stream()
                .mapToInt(SysRole::getDeptDataScope)
                .max().orElse(DataScopeDeptEnum.SELF.getValue());

        Set<Long> deptIds = new HashSet<>();
        switch (minScope) {
            case 2: // Custom departments
                List<SysRoleDept> roleDepts = roleDeptMapper.selectList(
                        new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, roleIds)
                );
                deptIds.addAll(roleDepts.stream().map(SysRoleDept::getDeptId).collect(Collectors.toSet()));
                break;
            case 3: // This department and sub-departments
                deptIds.addAll(getDeptAndChildren(user.getDeptIds()));
                break;
            case 4: // This department only
                deptIds.addAll(user.getDeptIds());
                break;
            case 5: // Self only
                return prefix + annotation.createByField() + " = '" + user.getUsername() + "'";
        }

        if (deptIds.isEmpty()) return "1=0";
        return prefix + annotation.deptIdField() + " IN (" +
                deptIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    private String buildRegionCondition(LoginUserDetails user, DataPermission annotation, String prefix) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId())
        );
        if (userRoles.isEmpty()) return "";

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);

        boolean hasAllRegion = roles.stream().anyMatch(r -> r.getRegionDataScope() == DataScopeRegionEnum.ALL.getValue());
        if (hasAllRegion) return "";

        int minScope = roles.stream()
                .mapToInt(SysRole::getRegionDataScope)
                .max().orElse(DataScopeRegionEnum.USER_REGION.getValue());

        Set<Long> regionIds = new HashSet<>();
        switch (minScope) {
            case 2: // Custom regions
                List<SysRoleRegion> roleRegions = roleRegionMapper.selectList(
                        new LambdaQueryWrapper<SysRoleRegion>().in(SysRoleRegion::getRoleId, roleIds)
                );
                regionIds.addAll(roleRegions.stream().map(SysRoleRegion::getRegionId).collect(Collectors.toSet()));
                break;
            case 3: // User's own regions
                regionIds.addAll(user.getRegionIds());
                break;
        }

        if (regionIds.isEmpty()) return "1=0";
        return prefix + annotation.regionIdField() + " IN (" +
                regionIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    private List<Long> getDeptAndChildren(List<Long> deptIds) {
        Set<Long> result = new HashSet<>(deptIds);
        List<SysDept> allDepts = deptMapper.selectList(null);
        for (Long deptId : deptIds) {
            collectChildren(allDepts, deptId, result);
        }
        return new ArrayList<>(result);
    }

    private void collectChildren(List<SysDept> allDepts, Long parentId, Set<Long> result) {
        allDepts.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .forEach(d -> {
                    result.add(d.getId());
                    collectChildren(allDepts, d.getId(), result);
                });
    }
}
