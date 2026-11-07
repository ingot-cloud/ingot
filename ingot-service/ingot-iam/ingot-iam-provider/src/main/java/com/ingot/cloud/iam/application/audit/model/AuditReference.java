package com.ingot.cloud.iam.application.audit.model;

import com.ingot.cloud.iam.domain.audit.AuditReferenceState;
import com.ingot.cloud.iam.domain.audit.AuditReferenceType;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>审计中出现的对象按查看人权限解析后的名称，不可见对象不返回名称。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param type 对象类型
 * @param id 对象 ID
 * @param state 披露状态
 * @param name 名称；仅可见或已删除且有快照时返回
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "审计中出现的对象按查看人权限解析后的名称")
public record AuditReference(
        @NotNull @Schema(description = "对象类型", requiredMode = Schema.RequiredMode.REQUIRED)
        AuditReferenceType type,
        @NotBlank @Schema(description = "对象 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Schema(description = "披露状态", requiredMode = Schema.RequiredMode.REQUIRED)
        AuditReferenceState state,
        @Schema(description = "名称；仅可见或已删除且有快照时返回")
        String name) {

    /**
     * 可见对象。
     *
     * @param type 类型
     * @param id ID
     * @param name 名称
     * @return 引用
     */
    public static AuditReference visible(AuditReferenceType type, String id, String name) {
        return new AuditReference(type, id, AuditReferenceState.VISIBLE, name);
    }

    /**
     * 无权查看的对象。
     *
     * @param type 类型
     * @param id ID
     * @return 引用
     */
    public static AuditReference hidden(AuditReferenceType type, String id) {
        return new AuditReference(type, id, AuditReferenceState.HIDDEN, null);
    }

    /**
     * 已删除对象。
     *
     * @param type 类型
     * @param id ID
     * @param snapshot 审计快照名称，可空
     * @return 引用
     */
    public static AuditReference deleted(AuditReferenceType type, String id, String snapshot) {
        return new AuditReference(type, id, AuditReferenceState.DELETED, snapshot);
    }
}
