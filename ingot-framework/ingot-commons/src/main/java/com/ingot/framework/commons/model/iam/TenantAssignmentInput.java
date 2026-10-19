package com.ingot.framework.commons.model.iam;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>按角色身份分配本组织成员或组，来源委派持续约束。</p>
 * @author jy
 * @since 1.0.0
 * @param subject 本组织成员或组
 * @param roleId 角色身份ID
 * @param scopeBindings 资源范围参数绑定
 * @param validFrom 可空UTC生效瞬时
 * @param validUntil 可空UTC截止瞬时
 * @param delegationGrantId 可空来源委派ID
 */
@Schema(description = "按角色身份分配本组织成员或组，来源委派持续约束", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record TenantAssignmentInput(
        @NotNull @Valid SubjectRef subject,
        @NotBlank String roleId,
        @NotNull Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings,
        Instant validFrom,
        Instant validUntil,
        String delegationGrantId) {

    /** 拒绝退出版本字段和未知输入，避免旧契约被静默解释。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("当前授权契约不支持字段：" + field); }

    /** 验证生效包含、截止不包含的合法瞬时间隔。 */
    @JsonIgnore
    @AssertTrue(message = "生效時間必须早于截止时间")
    public boolean isValidPeriod() { return validFrom == null || validUntil == null || validFrom.isBefore(validUntil); }

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public TenantAssignmentInput {
        if (scopeBindings != null) scopeBindings = Map.copyOf(scopeBindings);
    }
}
