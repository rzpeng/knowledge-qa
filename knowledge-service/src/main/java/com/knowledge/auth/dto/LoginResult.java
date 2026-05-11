package com.knowledge.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResult {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
    private String userName;
    private Integer isSuperAdmin;
    private List<String> permissions;

    // Manual getters/setters for Lombok compatibility with Gradle 9.5
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Integer getIsSuperAdmin() { return isSuperAdmin; }
    public void setIsSuperAdmin(Integer isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
