package com.knowledge.feishu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "feishu")
public class FeishuConfig {
    
    private String app_id;
    private String app_secret;
    private String bot_token;
    private String webhook_url;
    private boolean enabled = false;
    
    // Getters and Setters
    public String getApp_id() {
        return app_id;
    }
    
    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }
    
    public String getApp_secret() {
        return app_secret;
    }
    
    public void setApp_secret(String app_secret) {
        this.app_secret = app_secret;
    }
    
    public String getBot_token() {
        return bot_token;
    }
    
    public void setBot_token(String bot_token) {
        this.bot_token = bot_token;
    }
    
    public String getWebhook_url() {
        return webhook_url;
    }
    
    public void setWebhook_url(String webhook_url) {
        this.webhook_url = webhook_url;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}