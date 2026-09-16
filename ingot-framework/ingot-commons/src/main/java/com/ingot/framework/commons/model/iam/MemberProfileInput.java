package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>更新当前域成员资料，禁止携带凭证、状态或其它组织身份。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 成员读取版本
 * @param displayName 显示名，可空表示不修改
 * @param avatar 头像，可空表示不修改
 * @param phone 组织通讯录手机号，可空表示不修改；不可编辑或脱敏占位由服务拒绝
 * @param email 组织通讯录邮箱，可空表示不修改；不可编辑或脱敏占位由服务拒绝
 */
@Schema(description = "更新当前域成员资料，禁止携带凭证、状态或其它组织身份")
public record MemberProfileInput(
        @NotBlank @Schema(description = "成员读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @Schema(description = "显示名，可空表示不修改")
        String displayName,
        @Schema(description = "头像，可空表示不修改")
        String avatar,
        @Schema(description = "组织通讯录手机号，可空表示不修改")
        String phone,
        @Schema(description = "组织通讯录邮箱，可空表示不修改")
        String email) {
}
