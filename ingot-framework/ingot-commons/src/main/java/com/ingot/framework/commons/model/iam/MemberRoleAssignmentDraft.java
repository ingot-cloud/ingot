package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>创建平台成员时附带的固定版本角色分配草稿，由服务端绑定新成员主体并原子校验。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param roleId 角色定义 ID，用于重验所选固定版本仍属于该角色且可分配
 * @param roleRevisionRef 所选固定版本
 * @param scopeBindings 范围参数绑定
 * @param validFrom 生效瞬时，可空表示创建时立即生效
 * @param validUntil 失效瞬时，可空表示长期有效
 */
@Schema(description = "新平台成员的固定版本角色分配")
public record MemberRoleAssignmentDraft(
        @NotBlank String roleId,
        @NotNull @Valid RoleRevisionRef roleRevisionRef,
        @NotNull Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant validFrom,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant validUntil) {

    /**
     * 保持范围参数不可由调用方在校验和写入之间修改。
     */
    public MemberRoleAssignmentDraft {
        if (scopeBindings != null) {
            scopeBindings = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(scopeBindings));
        }
    }

    /**
     * 检查显式起止时间的先后关系。
     *
     * @return 左闭右开区间有效时为 true
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "起止时间必须形成左闭右开区间")
    @Schema(hidden = true)
    public boolean isValidPeriod() {
        return validFrom == null || validUntil == null || validFrom.isBefore(validUntil);
    }
}
