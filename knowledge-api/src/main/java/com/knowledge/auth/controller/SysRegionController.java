package com.knowledge.auth.controller;

import com.knowledge.auth.dto.R;
import com.knowledge.auth.entity.SysRegion;
import com.knowledge.auth.service.SysRegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/region")
@RequiredArgsConstructor
public class SysRegionController {
    private final SysRegionService regionService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:region:list')")
    public R<List<SysRegion>> list() {
        return R.ok(regionService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:region:list')")
    public R<SysRegion> get(@PathVariable Long id) {
        return R.ok(regionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:region:add')")
    public R<Void> save(@RequestBody SysRegion region) {
        regionService.save(region);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:region:edit')")
    public R<Void> update(@RequestBody SysRegion region) {
        regionService.update(region);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:region:delete')")
    public R<Void> delete(@PathVariable Long id) {
        regionService.delete(id);
        return R.ok();
    }
}
