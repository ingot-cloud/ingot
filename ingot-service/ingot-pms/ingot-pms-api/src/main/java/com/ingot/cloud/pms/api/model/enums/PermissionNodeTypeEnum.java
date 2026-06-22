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
 * <p>权限节点类型：分组、菜单导航、操作。</p>
 *
 * <p>NAVIGATION 由菜单托管生成，不可通过权限接口直接增删改。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum PermissionNodeTypeEnum {
    GROUP("0", "分组"),
    NAVIGATION("1", "导航"),
    ACTION("2", "操作");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, PermissionNodeTypeEnum> VALUE_MAP = new HashMap<>();

    static {
        for (PermissionNodeTypeEnum item : values()) {
            VALUE_MAP.put(item.value, item);
        }
    }

    @EnumDeserializeMethod
    @JsonCreator
    public static PermissionNodeTypeEnum getEnum(String value) {
        return VALUE_MAP.get(value);
    }
}
