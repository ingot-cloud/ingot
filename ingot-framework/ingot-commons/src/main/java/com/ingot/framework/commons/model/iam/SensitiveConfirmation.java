package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>嵌在敏感写请求体内的身份确认，不单独作为验密接口响应。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind 确认种类
 * @param secret 口令明文；传输由 HYBRID 信封保护
 */
@Schema(description = "嵌在敏感写请求体内的身份确认，不单独作为验密接口响应")
public record SensitiveConfirmation(
        @NotNull @Schema(description = "确认种类", requiredMode = Schema.RequiredMode.REQUIRED)
        SensitiveConfirmationKind kind,
        @NotBlank @Schema(description = "口令明文；传输由 HYBRID 信封保护", requiredMode = Schema.RequiredMode.REQUIRED)
        String secret) {
}
