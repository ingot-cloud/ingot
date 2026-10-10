package com.ingot.framework.commons.model.iam;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>资源字段固定版本，分别定义可见性和操作能力，缺失字段默认关闭。</p>
 * @param visibility 逻辑字段可见性
 * @param operations 逻辑字段操作能力，不独立授予操作或数据范围
 * @author jy
 * @since 1.0.0
 */
public record ResourceFieldDefinition(@NotNull Map<String, @NotNull FieldVisibility> visibility,
        @NotNull Map<String, @NotNull @Valid FieldOperations> operations) {
    /** 空权限定义，用于旧版本未登记的新字段。 */
    public static final ResourceFieldDefinition EMPTY = new ResourceFieldDefinition(Map.of(), Map.of());
    /** 冻结两个独立索引。 */
    public ResourceFieldDefinition {
        visibility = Map.copyOf(visibility);
        operations = Map.copyOf(operations);
    }
}
