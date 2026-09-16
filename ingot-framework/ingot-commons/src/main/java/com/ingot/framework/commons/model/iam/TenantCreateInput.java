package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p>提交组织创建所需的资料与所有者账号，基础目录引用由服务器生成。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param name 组织名称
 * @param ownerAccountId 已存在且有效的全局所有者账号
 * @param ownerDisplayName 所有者在本组织的显示名，可空时由服务生成
 * @param rootDepartmentName 根部门名称，可空时使用组织名称
 * @param avatar 组织头像，可空
 * @param planId 可选套餐；缺省时开通服务器标记的基础应用
 */
@Schema(description = "提交组织创建所需的资料与所有者账号，基础目录引用由服务器生成")
public record TenantCreateInput(
        @NotBlank @Size(max = 128) @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotBlank @Schema(description = "已存在且有效的全局所有者账号", requiredMode = Schema.RequiredMode.REQUIRED)
        String ownerAccountId,
        @Size(max = 128) @Schema(description = "所有者在本组织的显示名，可空时由服务生成")
        String ownerDisplayName,
        @Size(max = 128) @Schema(description = "根部门名称，可空时使用组织名称")
        String rootDepartmentName,
        @Schema(description = "组织头像，可空")
        String avatar,
        @Schema(description = "可选套餐；缺省时开通服务器标记的基础应用")
        String planId) {
}
