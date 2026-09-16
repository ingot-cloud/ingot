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
 * @param maxAssignmentDuration 单次分配最长持续时间，ISO-8601 duration，必须为正
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
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "单次分配最长持续时间，ISO-8601 duration，必须为正", type = "string",
                format = "duration", implementation = String.class, requiredMode = Schema.RequiredMode.REQUIRED)
        Duration maxAssignmentDuration) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public DelegationInput {
        if (allowedRoleRevisionRefs != null) {
            allowedRoleRevisionRefs = Collections.unmodifiableList(new ArrayList<>(allowedRoleRevisionRefs));
        }
        if (actionScopeCeilings != null) {
            actionScopeCeilings = Collections.unmodifiableList(new ArrayList<>(actionScopeCeilings));
        }
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
     * 最长分配期限必须为正值。
     *
     * @return 是否满足结构约束
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "最长分配期限必须为正值")
    @Schema(hidden = true)
    public boolean isPositiveDuration() {
        return maxAssignmentDuration == null || (!maxAssignmentDuration.isZero() && !maxAssignmentDuration.isNegative());
    }
}
