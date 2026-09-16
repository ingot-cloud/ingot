package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>引用当前域内的成员或组，引用解析必须验证其与可信授权上下文的归属关系。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param type 成员或组，必填
 * @param id 成员 ID 或组 ID，不使用账号 ID
 */
@Schema(description = "当前域内的授权主体引用，不携带切换域或租户的权限")
public record SubjectRef(
        @NotNull @Schema(description = "主体种类", requiredMode = Schema.RequiredMode.REQUIRED) SubjectType type,
        @NotBlank @Schema(description = "成员或组 ID", requiredMode = Schema.RequiredMode.REQUIRED) String id) {
}
