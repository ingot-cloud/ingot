package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * 租户添加成员结果；仅在新建全局账号时返回一次性初始密码。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 新成员 ID
 * @param version 创建后的版本
 * @param accountCreated 是否新建了全局账号
 * @param initialPassword 新建账号时的一次性明文；复用已有账号时省略
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "租户添加成员结果；仅新建账号时返回一次性初始密码")
public record TenantMemberCreated(
        @NotBlank @Schema(description = "新成员 ID", requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @NotBlank @Schema(description = "创建后的版本", requiredMode = Schema.RequiredMode.REQUIRED) String version,
        @NotNull @Schema(description = "是否新建了全局账号", requiredMode = Schema.RequiredMode.REQUIRED) Boolean accountCreated,
        @Schema(description = "新建账号时的一次性明文，HTTP 传输由信封加密") String initialPassword) {
}
