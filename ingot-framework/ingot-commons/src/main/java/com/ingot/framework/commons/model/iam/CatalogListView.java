package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>区分目录列表的分页视图和整树视图。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum CatalogListView {
    PAGE("page"),
    TREE("tree");

    /**
     * 分页列表视图的 YAML / query 字面量。
     */
    public static final String VALUE_PAGE = "page";
    /**
     * 整树视图的 YAML / query 字面量。
     */
    public static final String VALUE_TREE = "tree";

    /**
     * JSON 与查询参数使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, CatalogListView> BY_VALUE =
            EnumUtils.index(values(), CatalogListView::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static CatalogListView getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
