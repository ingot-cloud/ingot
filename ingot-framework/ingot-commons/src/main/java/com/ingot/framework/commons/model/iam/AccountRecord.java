package com.ingot.framework.commons.model.iam;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回全局账号自身状态，不携带凭证哈希或其他组织身份。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 全局账号 ID
 * @param username 登录名
 * @param phone 登录手机号，可空
 * @param email 登录邮箱，可空
 * @param enabled 是否启用
 * @param locked 是否锁定
 * @param mustChangePassword 是否必须改密
 * @param lastLoginAt 最近登录时间，UTC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回全局账号自身状态，不携带凭证哈希或其他组织身份")
public record AccountRecord(
        @NotBlank @Schema(description = "全局账号 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "登录名", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,
        @Schema(description = "登录手机号，可空")
        String phone,
        @Schema(description = "登录邮箱，可空")
        String email,
        @NotNull @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean enabled,
        @NotNull @Schema(description = "是否锁定", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean locked,
        @NotNull @Schema(description = "是否必须改密", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean mustChangePassword,
        @Schema(description = "最近登录时间，UTC")
        Instant lastLoginAt) {
}
