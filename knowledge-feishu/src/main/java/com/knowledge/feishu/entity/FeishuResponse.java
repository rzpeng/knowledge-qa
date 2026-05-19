package com.knowledge.feishu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuResponse {
    
    private int code;
    private String msg;
    private Object data;
    
    public static FeishuResponse success(Object data) {
        return new FeishuResponse(0, "success", data);
    }
    
    public static FeishuResponse error(int code, String msg) {
        return new FeishuResponse(code, msg, null);
    }
}