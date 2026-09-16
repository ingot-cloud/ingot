package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回当前成员身份的最小基础资料，不携带全局凭证或其他组织信息。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param memberId 当前成员 ID
 * @param displayName 当前域内显示名称
 * @param avatar 头像引用，可空
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回当前成员身份的最小基础资料，不携带全局凭证或其他组织信息")
public record CurrentProfile(
        @NotBlank @Schema(description = "当前成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String memberId,
        @NotBlank @Schema(description = "当前域内显示名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String displayName,
         @Schema(description = "头像引用，可空")
        String avatar) {
}
