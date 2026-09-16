package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>描述单个域内的成员资格，暂停或移出不改变全局账号及其他域成员的状态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MemberStatus {
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED"),
    REMOVED("REMOVED");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, MemberStatus> BY_VALUE = EnumUtils.index(values(), MemberStatus::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static MemberStatus getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
