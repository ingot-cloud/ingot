package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回已脱敏的授权变更审计，前后值只允许白名单配置字段。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 审计 ID
 * @param actor 操作者
 * @param context 操作时可信身份上下文
 * @param target 审计目标
 * @param changeType 变更类型
 * @param before 变更前的安全配置差异；不包含凭证或联系方式原值
 * @param after 变更后的安全配置差异
 * @param revisions 相关版本，字符串传输
 * @param delegationId 来源委派 ID，可空
 * @param assignmentId 关联授权 ID，可空
 * @param timestamp UTC 发生时间
 * @param traceId 链路标识，可空
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回已脱敏的授权变更审计，前后值只允许白名单配置字段")
public record AuditEntry(
        @NotBlank @Schema(description = "审计 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Valid @Schema(description = "操作者", requiredMode = Schema.RequiredMode.REQUIRED)
        AuditActor actor,
        @NotNull @Valid @Schema(description = "操作时可信身份上下文", requiredMode = Schema.RequiredMode.REQUIRED)
        AuthorizationContext context,
        @NotNull @Valid @Schema(description = "审计目标", requiredMode = Schema.RequiredMode.REQUIRED)
        AuditTarget target,
        @NotNull @Schema(description = "变更类型", requiredMode = Schema.RequiredMode.REQUIRED)
        AuditChangeType changeType,
        @NotNull @Schema(description = "变更前的安全配置差异；不包含凭证或联系方式原值", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotNull AuditField, String> before,
        @NotNull @Schema(description = "变更后的安全配置差异", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotNull AuditField, String> after,
        @NotNull @Schema(description = "相关版本，字符串传输", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotBlank String> revisions,
         @Schema(description = "来源委派 ID，可空")
        String delegationId,
         @Schema(description = "关联授权 ID，可空")
        String assignmentId,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 发生时间", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant timestamp,
         @Schema(description = "链路标识，可空")
        String traceId) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public AuditEntry {
        if (before != null) {
            before = Collections.unmodifiableMap(new LinkedHashMap<>(before));
        }
        if (after != null) {
            after = Collections.unmodifiableMap(new LinkedHashMap<>(after));
        }
        if (revisions != null) {
            revisions = Collections.unmodifiableMap(new LinkedHashMap<>(revisions));
        }
    }
}
