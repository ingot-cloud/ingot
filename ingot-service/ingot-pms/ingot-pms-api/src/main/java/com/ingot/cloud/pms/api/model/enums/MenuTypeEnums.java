package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : MenuTypeEnums.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/24.</p>
 * <p>Time         : 2:55 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum MenuTypeEnums {

    Directory("0", "目录"),
    Menu("1", "菜单"),
    Button("9", "按钮");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;
}
