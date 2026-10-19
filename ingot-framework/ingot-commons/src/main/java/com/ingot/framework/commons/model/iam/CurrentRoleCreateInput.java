package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>创建平台共享或组织私有角色，种类由受保护端点决定。</p>
 * @author jy
 * @since 1.0.0
 * @param code 稳定角色编码
 * @param name 角色名称
 * @param description 可空说明
 * @param groupName 可空展示分组
 * @param definition 完整当前定义
 */
@Schema(description = "创建平台共享或组织私有角色，种类由受保护端点决定", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CurrentRoleCreateInput(
        @NotBlank @Size(max = TenantAuthorizationConstants.ROLE_TEXT_LIMIT) String code,
        @NotBlank @Size(max = TenantAuthorizationConstants.ROLE_TEXT_LIMIT) String name,
        @Size(max = TenantAuthorizationConstants.ROLE_DESCRIPTION_LIMIT) String description,
        @Size(max = TenantAuthorizationConstants.ROLE_TEXT_LIMIT) String groupName,
        @NotNull @Valid CurrentRoleDefinition definition) {

    /** 拒绝退出字段及未知属性，避免旧版本请求被静默解释为当前角色。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("当前授权契约不支持字段：" + field);
    }
}
