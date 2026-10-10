package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import com.ingot.framework.commons.annotation.field.FieldUse;

/**
 * <p>服务实际接入的字段清单，不包含业务原值或可调用地址。</p>
 * @param resource 完整资源身份
 * @param version 内容版本
 * @param bindings 精确操作和用途的实际属性绑定
 * @author jy
 * @since 1.0.0
 */
public record FieldBindingManifest(ResourceKey resource, String version, List<Binding> bindings) {
    /** 复制不可变清单。 */
    public FieldBindingManifest {
        bindings = List.copyOf(bindings);
    }

    /**
     * <p>同一逻辑字段可以由多个 DTO 的不同 JSON 属性接入。</p>
     * @param actionCode 精确操作
     * @param use 实际用途
     * @param valueType DTO 类型标识
     * @param property JSON 属性
     * @param fieldKey 资源逻辑字段
     * @param textual 是否文本属性；非文本不开放脱敏
     * @author jy
     * @since 1.0.0
     */
    public record Binding(String actionCode, FieldUse use, String valueType, String property, String fieldKey, boolean textual) {
    }
}
