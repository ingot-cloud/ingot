package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回服务器解析后的最小初始化结果，不含治理版本或默认可被客户端回写的标识。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param name 将创建的组织名称
 * @param ownerAccountId 所有者账号 ID
 * @param ownerDisplayName 所有者显示名
 * @param rootDepartmentName 根部门名称
 * @param applications 将开通的基础或套餐应用
 * @param planId 使用的套餐，未选择时为空
 */
@Schema(description = "返回服务器解析后的最小初始化结果，不含治理版本或默认可被客户端回写的标识")
public record TenantPreviewResult(
        @NotBlank @Schema(description = "将创建的组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotBlank @Schema(description = "所有者账号 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String ownerAccountId,
        @NotBlank @Schema(description = "所有者显示名", requiredMode = Schema.RequiredMode.REQUIRED)
        String ownerDisplayName,
        @NotBlank @Schema(description = "根部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String rootDepartmentName,
        @NotNull @Schema(description = "将开通的基础或套餐应用", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ApplicationSummary> applications,
        @Schema(description = "使用的套餐，未选择时为空")
        String planId) {

    /**
     * 复制应用清单。
     */
    public TenantPreviewResult {
        if (applications != null) {
            applications = Collections.unmodifiableList(new ArrayList<>(applications));
        }
    }
}
