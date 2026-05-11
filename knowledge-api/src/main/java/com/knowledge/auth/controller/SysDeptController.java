package com.knowledge.auth.controller;

import com.knowledge.auth.dto.R;
import com.knowledge.auth.entity.SysDept;
import com.knowledge.auth.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {
    private final SysDeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.listTree());
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<SysDept>> list() {
        return R.ok(deptService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<SysDept> get(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    public R<Void> save(@RequestBody SysDept dept) {
        deptService.save(dept);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public R<Void> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}
