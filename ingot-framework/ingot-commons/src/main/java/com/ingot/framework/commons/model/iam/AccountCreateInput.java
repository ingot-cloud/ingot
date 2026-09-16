package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>创建全局登录账号，不自动授予任何成员资格。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param username 登录名
 * @param phone 登录手机号，可空
 * @param email 登录邮箱，可空
 */
@Schema(description = "创建全局登录账号，不自动授予任何成员资格")
public record AccountCreateInput(
        @NotBlank @Schema(description = "登录名", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,
        @Schema(description = "登录手机号，可空")
        String phone,
        @Schema(description = "登录邮箱，可空")
        String email) {
}
