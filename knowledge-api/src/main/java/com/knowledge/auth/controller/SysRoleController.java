package com.knowledge.auth.controller;

import com.knowledge.auth.dto.R;
import com.knowledge.auth.entity.SysRole;
import com.knowledge.auth.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {
    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<SysRole>> list() {
        return R.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<SysRole> get(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public R<Void> save(@RequestBody SysRole role) {
        roleService.save(role);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> update(@RequestBody SysRole role) {
        roleService.update(role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    // Menu assignment
    @PostMapping("/{roleId}/menus")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return R.ok();
    }

    @GetMapping("/{roleId}/menus")
    public R<List<Long>> getMenuIds(@PathVariable Long roleId) {
        return R.ok(roleService.getAssignedMenuIds(roleId));
    }

    // Department data scope assignment
    @PostMapping("/{roleId}/depts")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> assignDepts(@PathVariable Long roleId, @RequestBody List<Long> deptIds) {
        roleService.assignDepts(roleId, deptIds);
        return R.ok();
    }

    @GetMapping("/{roleId}/depts")
    public R<List<Long>> getDeptIds(@PathVariable Long roleId) {
        return R.ok(roleService.getAssignedDeptIds(roleId));
    }

    // Region data scope assignment
    @PostMapping("/{roleId}/regions")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> assignRegions(@PathVariable Long roleId, @RequestBody List<Long> regionIds) {
        roleService.assignRegions(roleId, regionIds);
        return R.ok();
    }

    @GetMapping("/{roleId}/regions")
    public R<List<Long>> getRegionIds(@PathVariable Long roleId) {
        return R.ok(roleService.getAssignedRegionIds(roleId));
    }
}
