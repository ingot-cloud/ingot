package com.ingot.cloud.iam.domain.audit;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>审计页可解析名称的对象类型，字面量与审计 target_type 一致。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuditReferenceType {
    MEMBER("member"),
    DEPARTMENT("department"),
    GROUP("group"),
    ROLE("role"),
    ROLE_REVISION("role-revision"),
    ASSIGNMENT("assignment"),
    DELEGATION("delegation"),
    TENANT("tenant"),
    ACCOUNT("account"),
    APPLICATION("application"),
    ROLE_APPLICATION_TASK("role-application-task");

    /** 引用键分隔符，键格式为 {@code type:id}。 */
    public static final String KEY_SEPARATOR = ":";

    /**
     * JSON 使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, AuditReferenceType> BY_VALUE =
            EnumUtils.index(values(), AuditReferenceType::getValue);

    /**
     * 生成页级引用字典的键。
     *
     * @param id 对象 ID
     * @return {@code type:id}
     */
    public String key(String id) {
        return value + KEY_SEPARATOR + id;
    }

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static AuditReferenceType getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

    /**
     * 按字面量查找，未知返回 {@code null}。
     *
     * @param value 字面量，可空
     * @return 对应枚举或 {@code null}
     */
    public static AuditReferenceType find(String value) {
        return value == null ? null : BY_VALUE.get(value);
    }
}
