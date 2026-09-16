package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述审计操作者身份，显示名称仅在允许披露时返回。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param accountId 全局账号 ID
 * @param memberId 操作时成员 ID
 * @param displayName 可披露显示名，可空
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "描述审计操作者身份，显示名称仅在允许披露时返回")
public record AuditActor(
        @NotBlank @Schema(description = "全局账号 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String accountId,
        @NotBlank @Schema(description = "操作时成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String memberId,
         @Schema(description = "可披露显示名，可空")
        String displayName) {
}
