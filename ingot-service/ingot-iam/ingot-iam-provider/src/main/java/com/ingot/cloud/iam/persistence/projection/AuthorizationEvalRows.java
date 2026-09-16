package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleDeltaOperation;

/**
 * <p>承载授权求值具名 SQL 的只读投影，列集与测试夹具及生产查询保持一致。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AuthorizationEvalRows {
    private AuthorizationEvalRows() {
    }

    /**
     * <p>一次有效分配求值所需的版本、范围绑定、委派来源与两端有效期截止。</p>
     *
     * @param revisionId 角色版本 ID
     * @param scopeBindings 范围参数绑定 JSON
     * @param delegationGrantId 委派来源；直接授权为空
     * @param validUntil 分配自身截止；不限期为空
     * @param delegationValidUntil 来源委派截止；无委派或不限期为空
     * @author jy
     * @since 1.0.0
     */
    public record Assignment(BigInteger revisionId, String scopeBindings, BigInteger delegationGrantId,
                             LocalDateTime validUntil, LocalDateTime delegationValidUntil) {
    }

    /**
     * <p>租户对某应用的开通命中数与最近开通截止。</p>
     *
     * @param hits 命中条数；0 表示未开通或成员不在人群
     * @param earliestExpiry 最近开通截止；存在不限期开通时为空
     * @author jy
     * @since 1.0.0
     */
    public record Entitlement(long hits, LocalDateTime earliestExpiry) {
    }

    /**
     * <p>已启用角色定义上的版本及其可选基础版本。</p>
     *
     * @param id 当前版本 ID
     * @param baseRevisionId 基础版本；完整定义时为空
     * @author jy
     * @since 1.0.0
     */
    public record Revision(BigInteger id, BigInteger baseRevisionId) {
    }

    /**
     * <p>角色版本上的单条操作授权。</p>
     *
     * @param actionId 操作 ID
     * @param scopes 范围表达式 JSON
     * @author jy
     * @since 1.0.0
     */
    public record Grant(BigInteger actionId, String scopes) {
    }

    /**
     * <p>租户角色版本相对基础的单操作差异。</p>
     *
     * @param actionId 操作 ID
     * @param operation 差异操作
     * @param scopes 范围表达式 JSON
     * @author jy
     * @since 1.0.0
     */
    public record Delta(BigInteger actionId, RoleDeltaOperation operation, String scopes) {
    }

    /**
     * <p>委派派生授权的操作范围上限。</p>
     *
     * @param actionId 操作 ID
     * @param scopes 范围表达式 JSON
     * @param scopeBindings 范围参数绑定 JSON
     * @author jy
     * @since 1.0.0
     */
    public record Ceiling(BigInteger actionId, String scopes, String scopeBindings) {
    }

    /**
     * <p>操作及其所属应用的启用与域信息。</p>
     *
     * @param code 精确操作码
     * @param enabled 操作是否启用
     * @param domain 所属应用授权域
     * @param appEnabled 所属应用是否启用
     * @param applicationId 所属应用 ID
     * @author jy
     * @since 1.0.0
     */
    public record Action(String code, Boolean enabled, AuthorizationDomain domain, Boolean appEnabled,
                         BigInteger applicationId) {
    }

    /**
     * <p>操作可被角色引用的前提：自身与所属应用/资源的启停、应用授权域和资源范围能力。</p>
     *
     * @param id 操作 ID
     * @param enabled 操作是否启用
     * @param domain 所属应用授权域
     * @param appEnabled 所属应用是否启用
     * @param resourceEnabled 所属资源是否启用
     * @param scopeCapabilities 资源支持的范围种类 JSON 数组
     * @author jy
     * @since 1.0.0
     */
    public record Capability(BigInteger id, Boolean enabled, AuthorizationDomain domain, Boolean appEnabled,
                             Boolean resourceEnabled, String scopeCapabilities) {
    }
}
