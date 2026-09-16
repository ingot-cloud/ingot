package com.ingot.cloud.iam.evaluation;

import java.math.BigInteger;
import java.util.List;

import com.ingot.cloud.iam.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationActionCeilingMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleDeltaMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleGrantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.model.iam.AudienceKind;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.SubjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>为授权求值读取有效分配、版本快照、开通与操作码，JOIN 仅走 Mapper 具名 SQL。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AuthorizationEvaluationRepository {
    private final IamRoleAssignmentMapper assignments;
    private final IamRoleGrantMapper grants;
    private final IamRoleDeltaMapper deltas;
    private final IamRoleRevisionMapper revisions;
    private final IamActionMapper actions;
    private final IamTenantAppEntitlementMapper entitlements;
    private final IamDelegationActionCeilingMapper ceilings;

    /**
     * 读取当前成员仍然有效的直接分配。
     *
     * @param actor 已验证身份
     * @return 版本、绑定、委派来源与有效期截止
     */
    public List<AuthorizationEvalRows.Assignment> listDirectAssignments(AuthorizationContext actor) {
        BigInteger memberId = new BigInteger(actor.memberId());
        if (actor.domain() == AuthorizationDomain.TENANT) {
            return assignments.listTenantDirect(actor.domain(), GrantStatus.ACTIVE, SubjectType.MEMBER,
                    new BigInteger(actor.tenantId()), memberId);
        }
        return assignments.listPlatformDirect(actor.domain(), GrantStatus.ACTIVE, SubjectType.MEMBER, memberId);
    }

    /**
     * 读取当前成员通过组主体展开的有效分配。
     *
     * @param actor 已验证身份
     * @return 版本、绑定、委派来源与有效期截止
     */
    public List<AuthorizationEvalRows.Assignment> listGroupAssignments(AuthorizationContext actor) {
        BigInteger memberId = new BigInteger(actor.memberId());
        if (actor.domain() == AuthorizationDomain.TENANT) {
            return assignments.listTenantGroup(actor.domain(), GrantStatus.ACTIVE, SubjectType.GROUP,
                    new BigInteger(actor.tenantId()), memberId);
        }
        return assignments.listPlatformGroup(actor.domain(), GrantStatus.ACTIVE, SubjectType.GROUP, memberId);
    }

    /**
     * 读取委派派生授权的操作上限。
     *
     * @param delegationId 委派 ID
     * @return 操作、范围与绑定
     */
    public List<AuthorizationEvalRows.Ceiling> listCeilings(BigInteger delegationId) {
        return ceilings.listByDelegation(delegationId);
    }

    /**
     * 读取启用角色定义上的版本及其基础版本。
     *
     * @param revisionId 角色版本 ID
     * @return 命中行；定义停用时为空
     */
    public List<AuthorizationEvalRows.Revision> listEnabledRevisions(BigInteger revisionId) {
        return revisions.listEnabled(revisionId);
    }

    /**
     * 读取指定版本的基础授权。
     *
     * @param revisionId 角色版本 ID
     * @return 操作与范围
     */
    public List<AuthorizationEvalRows.Grant> listGrants(BigInteger revisionId) {
        return grants.listByRevision(revisionId);
    }

    /**
     * 读取指定版本相对基础的差异。
     *
     * @param revisionId 角色版本 ID
     * @return 单操作差异
     */
    public List<AuthorizationEvalRows.Delta> listDeltas(BigInteger revisionId) {
        return deltas.listByRevision(revisionId);
    }

    /**
     * 读取操作码及所属应用的启用与域。
     *
     * @param actionId 操作 ID
     * @return 操作投影；不存在时为空
     */
    public List<AuthorizationEvalRows.Action> listActions(BigInteger actionId) {
        return actions.listWithApplication(actionId);
    }

    /**
     * 读取租户开通且当前成员落入人群时的命中数与最近开通截止。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @param memberId 租户成员 ID
     * @return 命中条数与最近截止；无命中时条数为 0
     */
    public AuthorizationEvalRows.Entitlement entitlement(BigInteger tenantId, BigInteger applicationId,
                                                         BigInteger memberId) {
        AuthorizationEvalRows.Entitlement row =
                entitlements.entitlementForMember(tenantId, applicationId, memberId, AudienceKind.ALL);
        return row == null ? new AuthorizationEvalRows.Entitlement(0, null) : row;
    }
}
