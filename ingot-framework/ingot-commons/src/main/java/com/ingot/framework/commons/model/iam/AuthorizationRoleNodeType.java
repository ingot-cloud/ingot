package com.ingot.framework.commons.model.iam;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>角色分配选择树的节点类型，只有版本节点可选。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuthorizationRoleNodeType {
    ROLE("ROLE"),
    REVISION("REVISION");

    /** JSON 与数据库使用的稳定节点类型。 */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, AuthorizationRoleNodeType> BY_VALUE =
            EnumUtils.index(values(), AuthorizationRoleNodeType::getValue);

    /**
     * 按稳定字面量解析；未知非空类型拒绝处理。
     * @param value 稳定字面量；null 返回 null
     * @return 对应节点类型
     */
    @JsonCreator
    public static AuthorizationRoleNodeType getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
