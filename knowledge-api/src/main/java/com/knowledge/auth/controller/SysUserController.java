package com.knowledge.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.auth.dto.R;
import com.knowledge.auth.entity.SysUser;
import com.knowledge.auth.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {
    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return R.ok(userService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<SysUser> get(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<Void> save(@RequestBody SysUser user) {
        userService.save(user);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> update(@RequestBody SysUser user) {
        userService.update(user);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    // Department assignment
    @PostMapping("/{userId}/depts")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignDepts(@PathVariable Long userId, @RequestBody Map<String, List<Long>> body) {
        userService.assignDepts(userId, body.get("deptIds"), body.get("leaderDeptIds"));
        return R.ok();
    }

    @GetMapping("/{userId}/depts")
    public R<List<Long>> getDeptIds(@PathVariable Long userId) {
        return R.ok(userService.getDeptIds(userId));
    }

    // Region assignment
    @PostMapping("/{userId}/regions")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignRegions(@PathVariable Long userId, @RequestBody List<Long> regionIds) {
        userService.assignRegions(userId, regionIds);
        return R.ok();
    }

    @GetMapping("/{userId}/regions")
    public R<List<Long>> getRegionIds(@PathVariable Long userId) {
        return R.ok(userService.getRegionIds(userId));
    }

    // Role assignment
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return R.ok();
    }

    @GetMapping("/{userId}/roles")
    public R<List<Long>> getRoleIds(@PathVariable Long userId) {
        return R.ok(userService.getRoleIds(userId));
    }
}
