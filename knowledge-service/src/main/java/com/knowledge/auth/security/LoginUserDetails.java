package com.knowledge.auth.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class LoginUserDetails implements UserDetails {
    private Long userId;
    private Long accountId;
    private String username;
    private String password;
    private String userName;
    private Integer isSuperAdmin;
    private List<String> permissions;
    private List<Long> deptIds;
    private List<Long> regionIds;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isSuperAdmin != null && isSuperAdmin == 1) {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            if (permissions != null) {
                permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }
            return authorities;
        }
        if (permissions == null) return List.of();
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // Manual getters/setters for Lombok compatibility with Gradle 9.5
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Integer getIsSuperAdmin() { return isSuperAdmin; }
    public void setIsSuperAdmin(Integer isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; }
}
