package com.knowledge.enums;

import lombok.Getter;

@Getter
public enum DocumentStatus {

    PROCESSING(0, "处理中"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败");

    private final int code;
    private final String desc;

    DocumentStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DocumentStatus of(int code) {
        for (DocumentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return FAILED;
    }
}
