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
 * <p>应用对租户的默认访问策略：无租户覆盖时开放或关闭。</p>
 *
 * <p>全局停用优先于本策略；租户显式覆盖不因到期回退到本值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AppDefaultAccessModeEnum {
    OPEN("0", "默认开放"),
    CLOSED("1", "默认关闭");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, AppDefaultAccessModeEnum> VALUE_MAP = new HashMap<>();

    static {
        for (AppDefaultAccessModeEnum item : values()) {
            VALUE_MAP.put(item.value, item);
        }
    }

    @EnumDeserializeMethod
    @JsonCreator
    public static AppDefaultAccessModeEnum getEnum(String value) {
        return VALUE_MAP.get(value);
    }
}
