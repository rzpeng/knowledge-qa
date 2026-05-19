package com.knowledge.feishu.service;

import com.knowledge.feishu.config.FeishuConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class FeishuTokenManager {

    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final long BUFFER_SECONDS = 300;

    @Autowired
    private FeishuConfig feishuConfig;

    @Autowired
    private RestTemplate restTemplate;

    private String cachedToken;
    private long expiresAt;

    public synchronized String getTenantAccessToken() {
        if (cachedToken != null && Instant.now().getEpochSecond() < expiresAt - BUFFER_SECONDS) {
            return cachedToken;
        }
        return refreshToken();
    }

    private String refreshToken() {
        log.info("Refreshing Feishu tenant_access_token...");

        Map<String, String> body = new HashMap<>();
        body.put("app_id", feishuConfig.getApp_id());
        body.put("app_secret", feishuConfig.getApp_secret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, request, Map.class);

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && Integer.valueOf(0).equals(responseBody.get("code"))) {
                cachedToken = (String) responseBody.get("tenant_access_token");
                int expire = responseBody.get("expire") instanceof Number
                        ? ((Number) responseBody.get("expire")).intValue() : 7200;
                expiresAt = Instant.now().getEpochSecond() + expire;
                log.info("Feishu tenant_access_token refreshed, expires in {}s", expire);
                return cachedToken;
            } else {
                String msg = responseBody != null ? (String) responseBody.get("msg") : "no response";
                Integer code = responseBody != null ? (Integer) responseBody.get("code") : -1;
                log.warn("Feishu token API error: code={}, msg={}", code, msg);
                throw new RuntimeException("Feishu token API error: code=" + code + ", msg=" + msg);
            }
        } catch (Exception e) {
            log.error("Error refreshing Feishu token", e);
            throw new RuntimeException("Error refreshing Feishu token", e);
        }
    }
}
