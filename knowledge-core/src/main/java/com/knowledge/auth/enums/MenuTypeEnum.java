package com.knowledge.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MenuTypeEnum {
    DIR(0, "目录"),
    MENU(1, "菜单"),
    BUTTON(2, "按钮");

    private final int value;
    private final String desc;
}
