package com.ingot.framework.commons.model.iam;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述角色身份白名单和资源范围的有限组织委派。</p>
 * @author jy
 * @since 1.0.0
 * @param administratorMemberId 授权管理员成员ID
 * @param allowedRoleIds 可分配角色身份集合
 * @param recipientSelection 接收人员和部门
 * @param resourceScopeCeilings 逐资源范围上限
 * @param validFrom 可空UTC生效瞬时
 * @param validUntil 可空UTC截止瞬时
 * @param maxAssignmentDuration 正的最长分配持续时间
 * @param assignmentDurationMode 仅允许LIMITED的期限模式
 */
@Schema(description = "描述角色身份白名单和资源范围的有限组织委派", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record TenantDelegationInput(
        @NotBlank String administratorMemberId,
        @NotEmpty List<@NotBlank String> allowedRoleIds,
        @NotNull @Valid Selection recipientSelection,
        @NotNull List<@NotNull @Valid ResourceScopeCeiling> resourceScopeCeilings,
        Instant validFrom,
        Instant validUntil,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) Duration maxAssignmentDuration,
        AssignmentDurationMode assignmentDurationMode) {

    /** 拒绝退出版本字段和未知输入，避免旧契约被静默解释。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("当前授权契约不支持字段：" + field); }

    /** 验证生效包含、截止不包含的合法瞬时间隔。 */
    @JsonIgnore
    @AssertTrue(message = "生效時間必须早于截止时间")
    public boolean isValidPeriod() { return validFrom == null || validUntil == null || validFrom.isBefore(validUntil); }

    /** 固定授权集合；省略期限模式时使用有限时长。 */
    public TenantDelegationInput {
        if (allowedRoleIds != null) allowedRoleIds = List.copyOf(allowedRoleIds);
        if (resourceScopeCeilings != null) resourceScopeCeilings = List.copyOf(resourceScopeCeilings);
        if (assignmentDurationMode == null) assignmentDurationMode = AssignmentDurationMode.LIMITED;
    }

    /** 组织来源只允许正的有限分配时长。 */
    @JsonIgnore
    @AssertTrue(message = "组织委派必须使用正的有限分配时长")
    public boolean isLimitedDuration() { return assignmentDurationMode == AssignmentDurationMode.LIMITED && maxAssignmentDuration != null && !maxAssignmentDuration.isNegative() && !maxAssignmentDuration.isZero(); }
}
