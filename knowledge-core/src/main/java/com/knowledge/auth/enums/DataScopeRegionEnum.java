package com.knowledge.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScopeRegionEnum {
    ALL(1, "全部地区"),
    CUSTOM(2, "自定义地区"),
    USER_REGION(3, "本用户所属地区");

    private final int value;
    private final String desc;
}
