package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>编辑角色资料与启停，技术锁不作为业务版本。</p>
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 当前配置锁
 * @param name 可选名称
 * @param description 可选说明
 * @param groupName 可选分组
 * @param status 启停状态
 */
@Schema(description = "编辑角色资料与启停，技术锁不作为业务版本", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CurrentRoleProfileInput(
        @NotBlank String expectedVersion,
        @Size(max = TenantAuthorizationConstants.ROLE_TEXT_LIMIT) String name,
        @Size(max = TenantAuthorizationConstants.ROLE_DESCRIPTION_LIMIT) String description,
        @Size(max = TenantAuthorizationConstants.ROLE_TEXT_LIMIT) String groupName,
        @NotNull ConfigurationStatus status) {

    /** 拒绝退出字段及未知属性，避免旧版本请求被静默解释为当前角色。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("当前授权契约不支持字段：" + field);
    }
}
