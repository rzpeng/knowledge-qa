package com.knowledge.feishu.entity;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FeishuMessage {
    
    private String msg_type;
    private String content;
    private List<Map<String, Object>> card;
    private String text;
    private String chat_id;
    private String message_id;
    private String open_id;
    private String union_id;
    private String tenant_key;
    private String event_type;
    private String header;
    private String body;
    
    // 消息类型枚举
    public enum MsgType {
        TEXT("text"),
        INTERACTIVE("interactive"),
        CARD("card"),
        SHARE_CHAT("share_chat"),
        POST("post");
        
        private final String value;
        
        MsgType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}