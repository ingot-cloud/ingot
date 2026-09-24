package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>提交组织创建所需的资料、所有者账号与开通选择，治理版本由服务器生成。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param name 组织名称
 * @param ownerAccountId 已存在且有效的全局所有者账号
 * @param ownerDisplayName 所有者在本组织的显示名，可空时由服务生成
 * @param rootDepartmentName 根部门名称，可空时使用组织名称
 * @param avatar 组织头像，可空；可提交时效链接或对象路径，入库只保存路径
 * @param planId 可选套餐；与自选应用取并集
 * @param applications 自选应用及期限覆盖，可空；不是完整目录
 */
@Schema(description = "提交组织创建所需的资料、所有者账号与开通选择，治理版本由服务器生成")
public record TenantCreateInput(
        @NotBlank @Size(max = 128) @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotBlank @Schema(description = "已存在且有效的全局所有者账号", requiredMode = Schema.RequiredMode.REQUIRED)
        String ownerAccountId,
        @Size(max = 128) @Schema(description = "所有者在本组织的显示名，可空时由服务生成")
        String ownerDisplayName,
        @Size(max = 128) @Schema(description = "根部门名称，可空时使用组织名称")
        String rootDepartmentName,
        @Schema(description = "组织头像，可空；可提交时效链接或对象路径，入库只保存路径")
        String avatar,
        @Schema(description = "可选套餐；与自选应用取并集")
        String planId,
        @Schema(description = "自选应用及期限覆盖，可空；不是完整目录")
        List<@NotNull @Valid EntitlementDraft> applications) {

    /**
     * 复制自选开通清单。
     */
    public TenantCreateInput {
        if (applications != null) {
            applications = Collections.unmodifiableList(new ArrayList<>(applications));
        }
    }
}
