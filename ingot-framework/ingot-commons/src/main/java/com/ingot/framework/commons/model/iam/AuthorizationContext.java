package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>描述认证后选定的唯一成员身份，平台和租户上下文采用互斥的租户 ID 约束。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param domain 当前管理域
 * @param tenantId 租户域必填，平台域必须为空
 * @param accountId 全局账号 ID，不是成员 ID
 * @param memberId 当前域内成员 ID，两域均必填
 * @apiNote 本类型只验证结构，不能证明身份可信；调用方必须先验证认证信息、成员归属和实时状态，
 * 不能将客户端反序列化的实例直接设为认证上下文。
 */
@Schema(description = "经认证选择的当前成员身份；不接受客户端直接指定权限域",
        accessMode = Schema.AccessMode.READ_ONLY)
public record AuthorizationContext(
        @Schema(description = "当前管理域", requiredMode = Schema.RequiredMode.REQUIRED)
        AuthorizationDomain domain,
        @Schema(description = "租户域必填；平台域为空") String tenantId,
        @Schema(description = "全局账号 ID", requiredMode = Schema.RequiredMode.REQUIRED) String accountId,
        @Schema(description = "当前域的成员 ID", requiredMode = Schema.RequiredMode.REQUIRED) String memberId) implements java.io.Serializable {

    /**
     * 校验域与身份标识的结构，不加载成员记录或授予权限。
     *
     * @throws IllegalArgumentException 域缺失、身份 ID 为空白或租户 ID 与域不匹配
     */
    public AuthorizationContext {
        if (domain == null || accountId == null || accountId.isBlank()
                || memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("管理域、账号 ID 和成员 ID 必填");
        }
        if (domain == AuthorizationDomain.PLATFORM && tenantId != null) {
            throw new IllegalArgumentException("平台身份不能携带租户 ID");
        }
        if (domain == AuthorizationDomain.TENANT && (tenantId == null || tenantId.isBlank())) {
            throw new IllegalArgumentException("租户身份必须携带租户 ID");
        }
    }
}
