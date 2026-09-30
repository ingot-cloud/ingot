package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回可见授权记录，保留主体、固定版本、范围及来源链。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 授权记录 ID
 * @param assignment 有效分配定义
 * @param status 生效或撤销状态；到期另按时间判断
 * @param source 写入来源
 * @param subjectName 接收对象名称
 * @param roleName 角色名称
 * @param revisionNumber 固定版本号
 * @param delegationSummary 授权依据摘要
 * @param createdAt 请求当地时间的授权时间
 * @param grantedBy 创建审计授权人
 * @param effectiveStatus 计算状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回可见授权记录，保留主体、固定版本、范围及来源链")
public record AssignmentRecord(
        @NotBlank @Schema(description = "授权记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Valid @Schema(description = "有效分配定义", requiredMode = Schema.RequiredMode.REQUIRED)
        AssignmentInput assignment,
        @NotNull @Schema(description = "生效或撤销状态；到期另按时间判断", requiredMode = Schema.RequiredMode.REQUIRED)
        GrantStatus status,
        @NotNull @Schema(description = "写入来源", requiredMode = Schema.RequiredMode.REQUIRED)
        AssignmentSource source,
        @Schema(description = "接收对象名称") String subjectName,
        @Schema(description = "角色名称") String roleName,
        @Schema(description = "固定版本号") String revisionNumber,
        @Schema(description = "授权依据摘要") String delegationSummary,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(
                using = com.ingot.framework.commons.jackson.WallClockInstantSerializer.class)
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
                using = com.ingot.framework.commons.jackson.WallClockInstantDeserializer.class)
        @Schema(description = "授权创建时间，按请求当地时区返回", type = "string", example = "2026-09-28 16:00:00")
        java.time.Instant createdAt,
        @Schema(description = "实际创建审计授权人") AssignmentAuthor grantedBy,
        @Schema(description = "计算后的生效状态") AssignmentEffectiveStatus effectiveStatus) {
    /**
     * 保持租户与历史投影构造方式兼容。
     * @param id 分配 ID
     * @param assignment 定义
     * @param status 持久化状态
     * @param source 来源
     */
    public AssignmentRecord(String id, AssignmentInput assignment, GrantStatus status, AssignmentSource source) {
        this(id, assignment, status, source, null, null, null, null, null, null, null);
    }
}
