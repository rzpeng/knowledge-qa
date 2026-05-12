package com.knowledge.auth.service;

import com.knowledge.auth.dto.LoginResult;
import com.knowledge.auth.security.JwtUtils;
import com.knowledge.auth.security.LoginUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public LoginResult login(String username, String password) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        LoginUserDetails user = (LoginUserDetails) auth.getPrincipal();

        String accessToken = jwtUtils.generateAccessToken(
                user.getUserId(), user.getAccountId(), user.getUsername(), user.getIsSuperAdmin(), user.getPermissions());
        String refreshToken = jwtUtils.generateRefreshToken(user.getAccountId());

        LoginResult result = new LoginResult();
        result.setAccessToken(accessToken);
        result.setRefreshToken(refreshToken);
        result.setUserId(user.getUserId());
        result.setUsername(user.getUsername());
        result.setUserName(user.getUserName());
        result.setIsSuperAdmin(user.getIsSuperAdmin());
        result.setPermissions(user.getPermissions());
        return result;
    }
}
