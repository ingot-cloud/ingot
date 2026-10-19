package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.ingot.framework.commons.model.iam.*;

/**
 * <p>承载租户当前角色授权的具名查询投影，平台固定版本查询不依赖这些字段。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantLiveRows {
    private TenantLiveRows() { }

    /** <p>组织当前拥有者及行政状态。</p> @author jy @since 1.0.0 */
    public record Owner(BigInteger ownerMemberId, BigInteger version, BigInteger memberVersion, BigInteger accountVersion) {
        /** 组织写锁仅包含行政事实。 */
        public Owner(BigInteger ownerMemberId,BigInteger version) { this(ownerMemberId,version,null,null); }
    }

    /** <p>参与当前视图指纹的策略与目录聚合标记。</p> @author jy @since 1.0.0 */
    public record Dependency(String category,String token) { }

    /** <p>角色身份、技术锁及服务器当前快照。</p> @author jy @since 1.0.0 */
    public record Role(BigInteger id, BigInteger tenantId, RoleKind kind, String code, String name,
                       String description, String groupName, Boolean enabled, BigInteger version,
                       BigInteger currentRevisionId) { }

    /** <p>操作及其真实应用资源能力。</p> @author jy @since 1.0.0 */
    public record Action(BigInteger actionId, String actionCode, String actionName, BigInteger applicationId,
                         String applicationName, BigInteger resourceId, String resourceName,
                         Boolean enabled, String scopeCapabilities, BigInteger actionVersion, BigInteger applicationVersion, BigInteger resourceVersion) { }

    /** <p>当前角色分配与历史不可变引用，旧引用仅作回滚证据。</p> @author jy @since 1.0.0 */
    public record Assignment(BigInteger id, BigInteger tenantId, SubjectType subjectType, BigInteger memberId,
                             BigInteger groupId, BigInteger roleId, RoleKind roleKind, BigInteger currentRevisionId,
                             String scopeBindings, BigInteger delegationGrantId, LocalDateTime validFrom,
                             LocalDateTime validUntil, GrantStatus status, AssignmentSource source,
                             BigInteger version, LocalDateTime createdAt, String subjectName, String roleName) { }

    /** <p>组织有限期限委派及管理员身份。</p> @author jy @since 1.0.0 */
    public record Delegation(BigInteger id, BigInteger tenantId, BigInteger administratorMemberId,
                            LocalDateTime validFrom, LocalDateTime validUntil, BigInteger durationSeconds,
                            Integer durationNanos, GrantStatus status, BigInteger version, String administratorName) { }

    /** <p>委派资源上限的JSON存储投影。</p> @author jy @since 1.0.0 */
    public record Ceiling(BigInteger resourceId, String scopes, String scopeBindings) { }

    /** <p>批量读取的不可变快照授权。</p> @author jy @since 1.0.0 */
    public record SnapshotGrant(BigInteger snapshotId,BigInteger actionId,String scopes) { }
    /** <p>批量读取的快照参数及资源身份。</p> @author jy @since 1.0.0 */
    public record SnapshotParameter(BigInteger snapshotId,String key,ScopeBindingKind kind,BigInteger resourceId) { }

    /** <p>角色参数及其资源归属。</p> @author jy @since 1.0.0 */
    public record Parameter(String key, ScopeBindingKind kind, BigInteger resourceId) { }

    /** <p>有效开通的应用身份及最近截止。</p> @author jy @since 1.0.0 */
    public record Application(BigInteger id, String code, String name, String icon, Integer sortOrder,
                              LocalDateTime expiresAt) { }

    /** <p>受限选择器的名称投影。</p> @author jy @since 1.0.0 */
    public record Option(BigInteger id, String name) { }
}
