package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>限定全局账号精确查找的用途，禁止用查找接口枚举组织关系。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AccountLookupPurpose {
    /**
     * 把已有账号关联为成员时使用，只返回标识与登录名。
     */
    MEMBER_CREATE("MEMBER_CREATE"),
    /**
     * 平台账号管理精确匹配，仍不返回组织资料。
     */
    ACCOUNT_MANAGE("ACCOUNT_MANAGE");

    /**
     * JSON 与请求使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, AccountLookupPurpose> BY_VALUE =
            EnumUtils.index(values(), AccountLookupPurpose::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static AccountLookupPurpose getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
