package com.ingot.framework.commons.model.iam;

/**
 * <p>当前资源精确操作的字段编辑和筛选结论，不独立授予对象范围。</p>
 * @author jy
 * @since 1.0.0
 * @param editable 是否允许更新该逻辑字段
 * @param filterable 是否允许将该逻辑字段作为原值筛选条件
 */
public record FieldOperations(boolean editable, boolean filterable) {
    /** 未声明能力时关闭所有操作。 */
    public static final FieldOperations NONE = new FieldOperations(false, false);
}
