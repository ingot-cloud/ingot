package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回当前认证账号的联系资料，不携带凭证或其他组织身份。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param accountId 当前认证账号 ID
 * @param username 登录名
 * @param phone 登录手机号，可空
 * @param email 登录邮箱，可空
 * @param mustChangePassword 是否必须改密
 * @param member 当前成员最小资料
 * @param version 当前账号资料版本，供 PATCH 携带 expectedVersion
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回当前认证账号的联系资料，不携带凭证或其他组织身份")
public record AccountSelfProfile(
        @NotBlank @Schema(description = "当前认证账号 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String accountId,
        @NotBlank @Schema(description = "登录名", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,
        @Schema(description = "登录手机号，可空")
        String phone,
        @Schema(description = "登录邮箱，可空")
        String email,
        @NotNull @Schema(description = "是否必须改密", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean mustChangePassword,
        @NotNull @Schema(description = "当前成员最小资料", requiredMode = Schema.RequiredMode.REQUIRED)
        CurrentProfile member,
        @NotBlank @Schema(description = "当前账号资料版本，供 PATCH 携带 expectedVersion", requiredMode = Schema.RequiredMode.REQUIRED)
        String version) {
}
