package com.ingot.cloud.iam.application.tenant.model;

import com.ingot.cloud.iam.application.shared.model.SensitiveConfirmation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>在当前组织内原子转交所有者职责。确认必须与转交出现在同一次请求。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 * @param newOwnerMemberId 当前组织内有效的新所有者成员 ID
 * @param confirmation 当前账号登录密码确认
 */
@Schema(description = "在当前组织内原子转交所有者职责。确认必须与转交出现在同一次请求")
public record OwnerTransferInput(
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Schema(description = "当前组织内有效的新所有者成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String newOwnerMemberId,
        @NotNull @Valid @Schema(description = "当前账号登录密码确认", requiredMode = Schema.RequiredMode.REQUIRED)
        SensitiveConfirmation confirmation) {
}
