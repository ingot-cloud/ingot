package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>记录对迁移歧义的显式处置，不自动扩大权限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MigrationDisposition {
    KEEP_MAPPED("KEEP_MAPPED"),
    NARROW("NARROW"),
    RECONFIGURE("RECONFIGURE"),
    ARCHIVE("ARCHIVE"),
    SKIP("SKIP");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, MigrationDisposition> BY_VALUE = EnumUtils.index(values(), MigrationDisposition::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static MigrationDisposition getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
