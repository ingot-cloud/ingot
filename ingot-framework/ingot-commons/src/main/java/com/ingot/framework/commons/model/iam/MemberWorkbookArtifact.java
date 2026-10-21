package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>标识表格任务可下载的产物，密码文件只能取一次。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MemberWorkbookArtifact {
    WORKBOOK("WORKBOOK"),
    ERRORS("ERRORS"),
    PASSWORDS("PASSWORDS");

    /**
     * JSON 查询参数字面量 {@code WORKBOOK}。
     */
    public static final String VALUE_WORKBOOK = "WORKBOOK";
    /**
     * JSON 查询参数字面量 {@code ERRORS}。
     */
    public static final String VALUE_ERRORS = "ERRORS";
    /**
     * JSON 查询参数字面量 {@code PASSWORDS}。
     */
    public static final String VALUE_PASSWORDS = "PASSWORDS";

    /**
     * JSON 与查询参数使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, MemberWorkbookArtifact> BY_VALUE =
            EnumUtils.index(values(), MemberWorkbookArtifact::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static MemberWorkbookArtifact getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
