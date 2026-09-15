package com.ingot.cloud.iam.api.model.dto.authorization;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;

/**
 * <p>用户在当前租户下的有效授权快照，供菜单、前端权限、API 鉴权与数据范围同源使用。</p>
 *
 * <p>禁止直接序列化角色领域类型；字段集合为冻结契约。空授权（无具体权限且无资源规则）
 * 不得写入热缓存；过期后刷新失败由调用方返回 503，不得伪装成成功空结果。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class AuthorizationSnapshotDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID。
     */
    private Long tenantId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 保留部门上下文的角色绑定。
     */
    private List<AuthorizationRoleBindingDTO> roleBindings = new ArrayList<>();

    /**
     * 当前启用的具体权限码。
     */
    private Set<String> permissionCodes = new LinkedHashSet<>();

    /**
     * 按资源与操作合并后的数据范围规则。
     */
    private List<AuthorizationResourceRuleDTO> resourceRules = new ArrayList<>();

    /**
     * 缓存来源，正常路径为 {@code REMOTE}。
     */
    private String source;

    /**
     * 快照版本，源端一致性读取开始时刻的纪元毫秒。
     */
    private Long version;

    /**
     * 源端一致性读取开始时刻。
     */
    private Instant generatedAt;

    /**
     * 绝对过期时刻，不超过 {@link #generatedAt} + 30 秒，且不跨越最近应用授权边界。
     */
    private Instant expiresAt;

    /**
     * 判断是否为空授权（无具体权限且无资源规则），此类结果不得写入热缓存。
     *
     * @return 空授权时返回 {@code true}
     */
    public boolean isEmptyAuthorization() {
        return (permissionCodes == null || permissionCodes.isEmpty())
                && (resourceRules == null || resourceRules.isEmpty());
    }
}
