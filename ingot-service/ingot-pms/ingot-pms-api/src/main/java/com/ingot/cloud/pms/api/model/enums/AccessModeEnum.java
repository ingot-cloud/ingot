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
 * <p>菜单访问模式：开放访问或需要权限校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AccessModeEnum {
    OPEN("0", "开放"),
    PERMISSION("1", "权限");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, AccessModeEnum> VALUE_MAP = new HashMap<>();

    static {
        for (AccessModeEnum item : values()) {
            VALUE_MAP.put(item.value, item);
        }
    }

    @EnumDeserializeMethod
    @JsonCreator
    public static AccessModeEnum getEnum(String value) {
        return VALUE_MAP.get(value);
    }
}
