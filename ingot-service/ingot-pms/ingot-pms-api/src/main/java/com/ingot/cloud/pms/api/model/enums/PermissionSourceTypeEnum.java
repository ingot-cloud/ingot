package com.ingot.cloud.pms.api.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.core.convert.EnumDeserializeMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>权限来源类型：系统内置、菜单托管或人工创建。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum PermissionSourceTypeEnum {
    SYSTEM("0", "系统"),
    MENU("1", "菜单"),
    MANUAL("2", "手工");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, PermissionSourceTypeEnum> VALUE_MAP = new HashMap<>();

    static {
        for (PermissionSourceTypeEnum item : values()) {
            VALUE_MAP.put(item.value, item);
        }
    }

    @EnumDeserializeMethod
    @JsonCreator
    public static PermissionSourceTypeEnum getEnum(String value) {
        return VALUE_MAP.get(value);
    }
}
