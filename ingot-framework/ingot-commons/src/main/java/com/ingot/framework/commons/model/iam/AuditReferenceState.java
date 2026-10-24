package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>审计引用对象对当前查看人的披露状态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuditReferenceState {
    /** 查看人可读取，返回名称。 */
    VISIBLE("VISIBLE"),
    /** 查看人无权读取，不返回名称。 */
    HIDDEN("HIDDEN"),
    /** 对象已不存在，可能返回审计快照名称。 */
    DELETED("DELETED");

    /**
     * JSON 使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, AuditReferenceState> BY_VALUE =
            EnumUtils.index(values(), AuditReferenceState::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static AuditReferenceState getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
