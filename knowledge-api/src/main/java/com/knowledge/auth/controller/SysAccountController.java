package com.knowledge.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.auth.dto.R;
import com.knowledge.auth.entity.SysAccount;
import com.knowledge.auth.service.SysAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/account")
@RequiredArgsConstructor
public class SysAccountController {
    private final SysAccountService accountService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:account:list')")
    public R<Page<SysAccount>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return R.ok(accountService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:account:list')")
    public R<SysAccount> get(@PathVariable Long id) {
        return R.ok(accountService.getById(id));
    }

    @GetMapping("/by-user/{userId}")
    public R<List<SysAccount>> listByUserId(@PathVariable Long userId) {
        return R.ok(accountService.listByUserId(userId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:account:add')")
    public R<Void> save(@RequestBody SysAccount account) {
        accountService.save(account);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:account:edit')")
    public R<Void> update(@RequestBody SysAccount account) {
        accountService.update(account);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:account:delete')")
    public R<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return R.ok();
    }
}
