package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>更新平台可见的组织实体，不返回或修改租户业务成员。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 组织读取版本
 * @param name 组织名称
 * @param avatar 组织头像，可空；可提交时效链接或对象路径，入库只保存路径
 * @param status 组织启停状态
 */
@Schema(description = "更新平台可见的组织实体，不返回或修改租户业务成员")
public record TenantUpdateInput(
        @NotBlank @Schema(description = "组织读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Size(max = 128) @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "组织头像，可空；可提交时效链接或对象路径，入库只保存路径")
        String avatar,
        @NotNull @Schema(description = "组织启停状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {
}
