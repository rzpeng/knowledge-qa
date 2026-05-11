package com.knowledge.auth.dto;

import lombok.Data;
import java.util.List;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
    private String userName;
    private Integer isSuperAdmin;
    private List<String> permissions;
}
