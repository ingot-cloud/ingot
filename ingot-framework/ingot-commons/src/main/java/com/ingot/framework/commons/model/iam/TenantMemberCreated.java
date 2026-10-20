package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * 租户添加或重新加入成员结果；仅在新建全局账号时返回一次性初始密码。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 成员 ID
 * @param version 提交后的版本
 * @param accountCreated 是否新建了全局账号
 * @param initialPassword 新建账号时的一次性明文；复用已有账号或重新加入时省略
 * @param rejoined 是否复用已移出成员行重新加入
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "租户添加或重新加入成员结果；仅新建账号时返回一次性初始密码")
public record TenantMemberCreated(
        @NotBlank @Schema(description = "成员 ID", requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @NotBlank @Schema(description = "提交后的版本", requiredMode = Schema.RequiredMode.REQUIRED) String version,
        @NotNull @Schema(description = "是否新建了全局账号", requiredMode = Schema.RequiredMode.REQUIRED) Boolean accountCreated,
        @Schema(description = "新建账号时的一次性明文，HTTP 传输由信封加密") String initialPassword,
        @NotNull @Schema(description = "是否复用已移出成员行重新加入", requiredMode = Schema.RequiredMode.REQUIRED) Boolean rejoined) {

    /**
     * 新建或加入在职路径的兼容构造，重新加入标记为 false。
     *
     * @param id 成员 ID
     * @param version 提交后的版本
     * @param accountCreated 是否新建了全局账号
     * @param initialPassword 一次性明文，可空
     */
    public TenantMemberCreated(String id, String version, Boolean accountCreated, String initialPassword) {
        this(id, version, accountCreated, initialPassword, Boolean.FALSE);
    }
}
