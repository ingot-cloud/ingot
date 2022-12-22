package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : RoleTypeEnums.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/22.</p>
 * <p>Time         : 12:46 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum RoleTypeEnums {

    System("0", "系统默认"),
    Custom("9", "自定义");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;
}
