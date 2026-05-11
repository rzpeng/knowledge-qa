package com.knowledge.auth.controller;

import com.knowledge.auth.dto.LoginRequest;
import com.knowledge.auth.dto.LoginResponse;
import com.knowledge.auth.dto.R;
import com.knowledge.auth.security.LoginUserDetails;
import com.knowledge.auth.service.AuthService;
import com.knowledge.auth.service.SysMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysMenuService menuService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.getUsername(), request.getPassword());
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(result.getAccessToken());
        resp.setRefreshToken(result.getRefreshToken());
        resp.setUserId(result.getUserId());
        resp.setUsername(result.getUsername());
        resp.setUserName(result.getUserName());
        resp.setIsSuperAdmin(result.getIsSuperAdmin());
        resp.setPermissions(result.getPermissions());
        return R.ok(resp);
    }

    @GetMapping("/userinfo")
    public R<LoginResponse> userinfo(@AuthenticationPrincipal LoginUserDetails user) {
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getUserId());
        resp.setUsername(user.getUsername());
        resp.setUserName(user.getUserName());
        resp.setIsSuperAdmin(user.getIsSuperAdmin());
        resp.setPermissions(user.getPermissions());
        return R.ok(resp);
    }

    @GetMapping("/menus")
    public R<List> menus(@AuthenticationPrincipal LoginUserDetails user) {
        List menus;
        if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1) {
            menus = menuService.getVisibleMenus();
        } else {
            menus = menuService.getMenuByPermissions(user.getPermissions());
        }
        return R.ok(menuService.buildTree(menus, 0L));
    }
}
