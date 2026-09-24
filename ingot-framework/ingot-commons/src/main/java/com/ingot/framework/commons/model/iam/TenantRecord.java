package com.ingot.framework.commons.model.iam;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回平台可管理的租户实体，不附带租户业务成员列表。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 租户 ID
 * @param name 组织名称
 * @param avatar 组织头像时效链接，可空
 * @param ownerMemberId 组织所有者成员 ID
 * @param ownerDisplayName 所有者在该组织内的显示名称；成员缺失或名为空时省略
 * @param ownerPhone 所有者在该组织内的手机号；成员缺失或为空时省略
 * @param ownerEmail 所有者在该组织内的邮箱；成员缺失或为空时省略
 * @param status 组织启停状态
 * @param createdAt 组织创建时间，UTC；缺失时省略
 * @param planId 最近一次提交的套餐，未选择时省略
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回平台可管理的租户实体，不附带租户业务成员列表")
public record TenantRecord(
        @NotBlank @Schema(description = "租户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "组织头像时效链接，可空")
        String avatar,
        @NotBlank @Schema(description = "组织所有者成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String ownerMemberId,
         @Schema(description = "所有者在该组织内的显示名称；成员缺失或名为空时省略")
        String ownerDisplayName,
         @Schema(description = "所有者在该组织内的手机号；成员缺失或为空时省略")
        String ownerPhone,
         @Schema(description = "所有者在该组织内的邮箱；成员缺失或为空时省略")
        String ownerEmail,
        @NotNull @Schema(description = "组织启停状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status,
         @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "组织创建时间，UTC；缺失时省略")
        Instant createdAt,
         @Schema(description = "最近一次提交的套餐，未选择时省略")
        String planId) {
}
