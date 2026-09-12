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
 * <p>受保护菜单的可见性匹配：关联权限满足任一或全部即可显示。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum PermissionMatchModeEnum {
    ANY("0", "任一权限"),
    ALL("1", "全部权限");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, PermissionMatchModeEnum> VALUE_MAP = new HashMap<>();

    static {
        for (PermissionMatchModeEnum item : values()) {
            VALUE_MAP.put(item.value, item);
        }
    }

    @EnumDeserializeMethod
    @JsonCreator
    public static PermissionMatchModeEnum getEnum(String value) {
        return VALUE_MAP.get(value);
    }
}
