package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>一次性返回管理员重置后的明文初始密码，不得再被列表或详情读取。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param password 新生成的初始密码明文
 */
@Schema(description = "一次性返回管理员重置后的明文初始密码，不得再被列表或详情读取")
public record AccountSecret(
        @NotBlank @Schema(description = "新生成的初始密码明文", requiredMode = Schema.RequiredMode.REQUIRED)
        String password) {
}
