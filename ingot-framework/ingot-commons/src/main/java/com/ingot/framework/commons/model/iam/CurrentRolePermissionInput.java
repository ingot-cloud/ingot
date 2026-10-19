package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>用配置锁保存角色完整当前权限。</p>
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 当前配置锁
 * @param definition 完整权限定义
 */
@Schema(description = "用配置锁保存角色完整当前权限", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CurrentRolePermissionInput(
        @NotBlank String expectedVersion,
        @NotNull @Valid CurrentRoleDefinition definition) {

    /** 拒绝退出字段及未知属性，避免旧版本请求被静默解释为当前角色。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("当前授权契约不支持字段：" + field);
    }
}
