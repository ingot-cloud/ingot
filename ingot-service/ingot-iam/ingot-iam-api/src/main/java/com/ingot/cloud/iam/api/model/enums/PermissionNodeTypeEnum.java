package com.ingot.cloud.iam.api.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.core.convert.EnumDeserializeMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>权限节点类型：通配分组或具体操作。</p>
 *
 * <p>GROUP 编码必须以 {@code :**} 结尾；ACTION 必须为不含通配的精确编码。取值 {@code 0}/{@code 2} 与库内历史值对齐，不使用 {@code 1}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum PermissionNodeTypeEnum {
    GROUP("0", "分组"),
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
