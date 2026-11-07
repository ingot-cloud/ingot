package com.ingot.cloud.iam.application.group.model;

import com.ingot.cloud.iam.application.organization.model.MemberRecord;
import com.ingot.cloud.iam.domain.group.GroupMembership;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回租户组当前页有效成员及其来源，成员资料已经字段策略投影。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param member 投影后的成员资料
 * @param membership 直接绑定与命中的部门规则
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "租户组有效成员及其来源，成员资料已经字段策略投影")
public record GroupEffectiveMemberRecord(
        @NotNull @Valid @Schema(description = "投影后的成员资料", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberRecord member,
        @NotNull @Valid @Schema(description = "直接绑定与命中的部门规则", requiredMode = Schema.RequiredMode.REQUIRED)
        GroupMembership membership) {
}
