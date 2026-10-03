package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述单条不可拼接的委派限制，角色、人群、范围与期限共同生效。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param administratorMemberId 当前域的授权管理员成员 ID
 * @param allowedRoleRevisionRefs 允许分配的固定角色版本集合
 * @param recipientSelection 允许接收的人群，当前域内解析
 * @param actionScopeCeilings 逐操作范围上限
 * @param validFrom UTC 生效时间，包含；空值无起始边界
 * @param validUntil UTC 失效时间，不包含；空值无结束边界
 * @param maxAssignmentDuration 有限模式的正持续时间；不限模式为空
 * @param assignmentDurationMode 单次期限模式；省略兼容有限期限
 */
@Schema(description = "描述单条不可拼接的委派限制，角色、人群、范围与期限共同生效")
public record DelegationInput(
        @NotBlank @Schema(description = "当前域的授权管理员成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String administratorMemberId,
        @NotEmpty @Schema(description = "允许分配的固定角色版本集合", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleRevisionRef> allowedRoleRevisionRefs,
        @NotNull @Valid @Schema(description = "允许接收的人群，当前域内解析", requiredMode = Schema.RequiredMode.REQUIRED)
        Selection recipientSelection,
        @NotNull @Schema(description = "逐操作范围上限", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ActionScopeCeiling> actionScopeCeilings,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 生效时间，包含；空值无起始边界")
        Instant validFrom,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 失效时间，不包含；空值无结束边界")
        Instant validUntil,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "有限模式必填的正持续时间；不限模式为空", type = "string",
                format = "duration", implementation = String.class)
        Duration maxAssignmentDuration,
        @Schema(description = "单次期限模式，省略按 LIMITED", defaultValue = "LIMITED")
        AssignmentDurationMode assignmentDurationMode) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public DelegationInput {
        if (assignmentDurationMode == null) assignmentDurationMode = AssignmentDurationMode.LIMITED;
        if (allowedRoleRevisionRefs != null) {
            allowedRoleRevisionRefs = Collections.unmodifiableList(new ArrayList<>(allowedRoleRevisionRefs));
        }
        if (actionScopeCeilings != null) {
            actionScopeCeilings = Collections.unmodifiableList(new ArrayList<>(actionScopeCeilings));
        }
    }

    /**
     * 保持既有有限期限调用的构造契约。
     * @param administratorMemberId 授权管理员
     * @param allowedRoleRevisionRefs 允许的固定版本
     * @param recipientSelection 接收人群
     * @param actionScopeCeilings 全部操作上限
     * @param validFrom 来源生效瞬时
     * @param validUntil 来源截止瞬时
     * @param maxAssignmentDuration 正的最长持续时间
     */
    public DelegationInput(String administratorMemberId, List<RoleRevisionRef> allowedRoleRevisionRefs,
            Selection recipientSelection, List<ActionScopeCeiling> actionScopeCeilings,
            Instant validFrom, Instant validUntil, Duration maxAssignmentDuration) {
        this(administratorMemberId, allowedRoleRevisionRefs, recipientSelection, actionScopeCeilings,
                validFrom, validUntil, maxAssignmentDuration, AssignmentDurationMode.LIMITED);
    }

    /**
     * 起止时间必须形成左闭右开的有效区间。
     *
     * @return 是否满足结构约束
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "起止时间必须形成左闭右开的有效区间")
    @Schema(hidden = true)
    public boolean isValidPeriod() {
        return validFrom == null || validUntil == null || validFrom.isBefore(validUntil);
    }

    /**
     * 有限模式的最长分配期限为正值，不限模式不携带时长。
     *
     * @return 是否满足结构约束
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "有限模式需要正的最长分配期限，不限模式不得携带时长")
    @Schema(hidden = true)
    public boolean isPositiveDuration() {
        return assignmentDurationMode == AssignmentDurationMode.UNLIMITED ? maxAssignmentDuration == null
                : maxAssignmentDuration != null && !maxAssignmentDuration.isZero() && !maxAssignmentDuration.isNegative();
    }
}
