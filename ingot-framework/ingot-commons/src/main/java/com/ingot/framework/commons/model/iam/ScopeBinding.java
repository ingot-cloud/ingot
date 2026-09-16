package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>承载授权命名参数的类型化 ID 集合，不接受客户端自定义范围表达式。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind 集合对象类型，必填
 * @param ids 字符串 ID 集合；空集合不产生对象范围，归属与合法性由服务端验证
 */
@Schema(description = "命名范围参数的类型化值")
public record ScopeBinding(
        @NotNull @Schema(description = "部门或对象集合类型", requiredMode = Schema.RequiredMode.REQUIRED)
        ScopeBindingKind kind,
        @NotNull @Schema(description = "字符串 ID 集合", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> ids) {

    /**
     * 复制参数集合，避免调用方在校验后修改其内容；空引用交由 Bean Validation 拒绝。
     */
    public ScopeBinding {
        if (ids != null) {
            ids = Collections.unmodifiableList(new ArrayList<>(ids));
        }
    }
}
