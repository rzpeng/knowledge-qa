package com.knowledge.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScopeDeptEnum {
    ALL(1, "全部数据"),
    CUSTOM(2, "自定义部门"),
    DEPT_AND_SUB(3, "本部门及下属"),
    DEPT_ONLY(4, "本部门"),
    SELF(5, "本人");

    private final int value;
    private final String desc;
}
