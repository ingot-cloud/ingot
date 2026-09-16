package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>更新全局账号联系资料，禁止改写凭证、锁定态或组织身份。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 读取获得的账号版本
 * @param phone 登录手机号，可空表示不修改
 * @param email 登录邮箱，可空表示不修改
 */
@Schema(description = "更新全局账号联系资料，禁止改写凭证、锁定态或组织身份")
public record AccountUpdateInput(
        @NotBlank @Schema(description = "读取获得的账号版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @Schema(description = "登录手机号，可空表示不修改")
        String phone,
        @Schema(description = "登录邮箱，可空表示不修改")
        String email) {
}
