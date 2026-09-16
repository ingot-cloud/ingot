package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>标记迁移批次处理阶段，只有完成所有门禁才能进入 VERIFIED。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MigrationBatchStatus {
    NEW("NEW"),
    PREFLIGHTED("PREFLIGHTED"),
    IMPORTED("IMPORTED"),
    VERIFIED("VERIFIED"),
    FAILED("FAILED");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, MigrationBatchStatus> BY_VALUE = EnumUtils.index(values(), MigrationBatchStatus::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static MigrationBatchStatus getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
