package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.SysMenu;
import com.knowledge.auth.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuService {
    private final SysMenuMapper menuMapper;

    public List<SysMenu> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));
    }

    public List<SysMenu> listTree() {
        List<SysMenu> all = listAll();
        return buildTree(all, 0L);
    }

    public List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        List<SysMenu> children = all.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .collect(Collectors.toList());
        for (SysMenu menu : children) {
            menu.setChildren(buildTree(all, menu.getId()));
        }
        return children;
    }

    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    public void save(SysMenu menu) {
        menuMapper.insert(menu);
    }

    public void update(SysMenu menu) {
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        Long count = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (count > 0) {
            throw new RuntimeException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }

    /** 根据权限标识列表获取菜单 */
    public List<SysMenu> getMenuByPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) return List.of();
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getPermission, permissions)
                        .orderByAsc(SysMenu::getSortOrder));
    }

    /** 获取所有可见菜单（非按钮类型） */
    public List<SysMenu> getVisibleMenus() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .ne(SysMenu::getType, 2)
                        .orderByAsc(SysMenu::getSortOrder));
    }
}
