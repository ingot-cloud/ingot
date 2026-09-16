package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回已按字段策略投影的资源详情、字段访问说明及当前对象操作能力。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param <T> 经过授权投影的资源类型
 * @param record 资源内容，隐藏字段不得包含原值
 * @param fieldAccess 字段访问结果
 * @param capabilities 按操作代码索引的对象能力，仅用于展示，提交时必须重验
 * @param version 求值版本字符串
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceDetail<T>(@NotNull @Valid T record,
        @NotNull Map<@NotBlank String, @NotNull @Valid FieldAccess> fieldAccess,
        @NotNull Map<@NotBlank String, @NotNull @Valid ObjectCapability> capabilities,
        @NotBlank String version) {
    /**
     * 创建详情并保留访问说明快照；缺失集合交由 Bean Validation 拒绝。
     */
    public ResourceDetail {
        fieldAccess = fieldAccess == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(fieldAccess));
        capabilities = capabilities == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(capabilities));
    }
}
