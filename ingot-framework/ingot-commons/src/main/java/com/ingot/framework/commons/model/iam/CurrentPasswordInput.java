package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>当前认证账号改密，初始改密可不提供旧密码，禁止提交其他账号 ID。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param oldPassword 旧密码；强制改密时可空
 * @param newPassword 新密码
 * @param confirmPassword 确认密码
 */
@Schema(description = "当前认证账号改密，初始改密可不提供旧密码，禁止提交其他账号 ID")
public record CurrentPasswordInput(
        @Schema(description = "旧密码；强制改密时可空")
        String oldPassword,
        @NotBlank @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword,
        @NotBlank @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED)
        String confirmPassword) {
}
