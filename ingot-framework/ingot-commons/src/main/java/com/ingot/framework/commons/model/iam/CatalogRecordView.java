package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>区分目录管理完整记录与选择器摘要投影。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum CatalogRecordView {
    CATALOG("CATALOG"),
    SUMMARY("SUMMARY");

    /**
     * 目录管理完整记录的 query 字面量。
     */
    public static final String VALUE_CATALOG = "CATALOG";
    /**
     * 选择器摘要投影的 query 字面量。
     */
    public static final String VALUE_SUMMARY = "SUMMARY";

    /**
     * JSON 与查询参数使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, CatalogRecordView> BY_VALUE =
            EnumUtils.index(values(), CatalogRecordView::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static CatalogRecordView getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
