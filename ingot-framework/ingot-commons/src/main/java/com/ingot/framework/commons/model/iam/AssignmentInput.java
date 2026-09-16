package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述单条角色分配输入，角色、主体、范围与来源委派须在提交时共同校验。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param subject 当前域接收主体
 * @param roleRevisionRef 固定角色版本引用
 * @param scopeBindings 命名参数绑定，未使用参数时为空对象
 * @param validFrom UTC 生效时间，包含；为空时由提交时间补齐
 * @param validUntil UTC 失效时间，不包含；为空表示长期但仍受委派约束
 * @param delegationGrantId 来源委派 ID，治理直接分配时可空
 */
@Schema(description = "描述单条角色分配输入，角色、主体、范围与来源委派须在提交时共同校验")
public record AssignmentInput(
        @NotNull @Valid @Schema(description = "当前域接收主体", requiredMode = Schema.RequiredMode.REQUIRED)
        SubjectRef subject,
        @NotNull @Valid @Schema(description = "固定角色版本引用", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleRevisionRef roleRevisionRef,
        @NotNull @Schema(description = "命名参数绑定，未使用参数时为空对象", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 生效时间，包含；为空时由提交时间补齐")
        Instant validFrom,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 失效时间，不包含；为空表示长期但仍受委派约束")
        Instant validUntil,
        @Schema(description = "来源委派 ID，治理直接分配时可空")
        String delegationGrantId) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public AssignmentInput {
        if (scopeBindings != null) {
            scopeBindings = Collections.unmodifiableMap(new LinkedHashMap<>(scopeBindings));
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
}
