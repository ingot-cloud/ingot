package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>用技术锁编辑完整组织委派定义。</p>
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 当前技术配置锁
 * @param delegation 完整委派限制
 */
@Schema(description = "用技术锁编辑完整组织委派定义", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record TenantDelegationUpdateInput(
        @NotBlank String expectedVersion,
        @NotNull @Valid TenantDelegationInput delegation) {

    /** 拒绝退出版本字段和未知输入，避免旧契约被静默解释。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("当前授权契约不支持字段：" + field); }
}
