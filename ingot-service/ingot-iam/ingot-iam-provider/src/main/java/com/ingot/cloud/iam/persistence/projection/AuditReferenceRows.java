package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;

import com.ingot.framework.commons.model.iam.SubjectType;

/**
 * <p>授权审计名称解析的只读投影，字段顺序与查询列一致。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AuditReferenceRows {
    private AuditReferenceRows() {
    }

    /** <p>对象名称。</p> @author jy @since 1.0.0 @param id 对象 ID @param name 名称 */
    public record Name(BigInteger id, String name) { }

    /**
     * <p>角色版本及所属角色名称。</p>
     *
     * @author jy
     * @since 1.0.0
     * @param id 版本 ID
     * @param roleName 角色名称
     * @param revision 版本序号
     * @param versionName 版本名称，可空
     */
    public record Revision(BigInteger id, String roleName, BigInteger revision, String versionName) { }

    /**
     * <p>角色分配的角色与接收对象。</p>
     *
     * @author jy
     * @since 1.0.0
     * @param id 分配 ID
     * @param roleId 角色 ID
     * @param subjectType 接收对象类型
     * @param memberId 接收成员 ID，可空
     * @param groupId 接收用户组 ID，可空
     */
    public record Assignment(BigInteger id, BigInteger roleId, SubjectType subjectType, BigInteger memberId,
                             BigInteger groupId) { }

    /** <p>委派及管理员成员。</p> @author jy @since 1.0.0 @param id 委派 ID @param memberId 管理员成员 ID */
    public record Delegation(BigInteger id, BigInteger memberId) { }

    /**
     * <p>角色版本应用任务。</p>
     *
     * @author jy
     * @since 1.0.0
     * @param id 任务 ID
     * @param roleId 角色 ID
     * @param revisionId 目标版本 ID
     */
    public record Task(BigInteger id, BigInteger roleId, BigInteger revisionId) { }

    /** <p>账号在当前租户内的成员。</p> @author jy @since 1.0.0 @param accountId 账号 ID @param memberId 成员 ID */
    public record AccountMember(BigInteger accountId, BigInteger memberId) { }
}
