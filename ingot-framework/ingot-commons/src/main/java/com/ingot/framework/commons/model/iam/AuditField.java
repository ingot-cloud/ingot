package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>限定审计差异可携带的配置字段，不包含凭证或通讯录敏感原值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuditField {
    NAME("NAME"),
    STATUS("STATUS"),
    ROLE_REVISION("ROLE_REVISION"),
    SCOPE("SCOPE"),
    RECIPIENT_SELECTION("RECIPIENT_SELECTION"),
    VALID_FROM("VALID_FROM"),
    VALID_UNTIL("VALID_UNTIL"),
    ENTITLEMENT("ENTITLEMENT"),
    POLICY_VERSION("POLICY_VERSION"),
    OWNER_MEMBER("OWNER_MEMBER");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, AuditField> BY_VALUE = EnumUtils.index(values(), AuditField::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static AuditField getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
