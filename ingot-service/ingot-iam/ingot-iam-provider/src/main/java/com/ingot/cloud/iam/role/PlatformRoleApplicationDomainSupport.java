package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleApplicationTaskEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAdmission;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput;
import com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>将平台自定义角色的应用规则接入共用流程，复用既有身份、范围与分配门禁。</p>
 * <p>现有任务仅保存平台身份，不支持租户或其他主体；调用方持有业务事务。</p>
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
final class PlatformRoleApplicationDomainSupport implements RoleApplicationDomainSupport {
    private final IamAccess access;
    private final RoleService roles;
    private final RoleRepository roleStore;
    private final AssignmentRepository assignmentStore;
    private final AssignmentService assignments;
    private final ResourceAccess resourceAccess;
    private final ActiveIdentityService identities;
    private final IamActionAuthorizer authorizer;

    /** {@inheritDoc} */
    @Override
    public IamAdmission admit() {
        var admission = access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE);
        requirePlatform(admission);
        if (!admission.governed()) throw new BizException(IamReasonCode.ACTION_DENIED);
        return admission;
    }

    /** {@inheritDoc} */
    @Override
    public IamAdmission restore(IamRoleApplicationTaskEntity task) {
        var context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, task.getActorAccountId().toString(),
                task.getActorMemberId().toString());
        var actor = identities.requireActive(context);
        var admission = new IamAdmission(actor, false);
        requirePlatform(admission);
        if (!authorizer.admit(actor.context(), IamAction.PLATFORM_ASSIGNMENT_UPGRADE).governed())
            throw new BizException(IamReasonCode.ACTION_DENIED);
        return new IamAdmission(actor, true);
    }

    /** {@inheritDoc} */
    @Override
    public ObjectScope scope(IamAdmission admission, boolean fresh) {
        requirePlatform(admission);
        return fresh ? resourceAccess.objectsForExecution(admission.actor().context(), IamAction.PLATFORM_ASSIGNMENT_UPGRADE)
                : resourceAccess.objects(admission.actor().context(), IamAction.PLATFORM_ASSIGNMENT_UPGRADE);
    }

    /** {@inheritDoc} */
    @Override
    public void requireRole(String roleId, IamAdmission admission, boolean fresh) {
        requirePlatform(admission);
        var role = roleStore.findDefinition(AuthorizationDomain.PLATFORM, false, null, IamIds.require(roleId));
        if (role == null || role.getKind() != RoleKind.PLATFORM_CUSTOM) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        var objects = fresh ? resourceAccess.objectsForExecution(admission.actor().context(), IamAction.PLATFORM_ROLE_READ)
                : resourceAccess.objects(admission.actor().context(), IamAction.PLATFORM_ROLE_READ);
        if (!ObjectScopeSql.matches(objects, IamIds.require(roleId))) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
    }

    /** {@inheritDoc} */
    @Override
    public void lockRole(long roleId) {
        assignmentStore.lockAuthorization(AuthorizationDomain.PLATFORM);
        roleStore.lockDefinition(AuthorizationDomain.PLATFORM, false, null, roleId);
    }

    /** {@inheritDoc} */
    @Override
    public CreatedResource publish(String roleId, RolePublishInput input) {
        return roles.publish(AuthorizationDomain.PLATFORM, false, roleId, input);
    }

    /** {@inheritDoc} */
    @Override
    public IamRoleRevisionEntity requireTarget(String roleId, String revisionId) {
        var target = findRevision(IamIds.require(revisionId));
        if (target == null || target.getRoleId().longValueExact() != IamIds.require(roleId)
                || target.getKind() != RoleKind.PLATFORM_CUSTOM)
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        return target;
    }

    /** {@inheritDoc} */
    @Override
    public IamRoleRevisionEntity findRevision(long revisionId) {
        return roleStore.findRevision(revisionId);
    }

    /** {@inheritDoc} */
    @Override
    public RoleRevisionRef reference(BigInteger revisionId) {
        return new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, revisionId.toString());
    }

    /** {@inheritDoc} */
    @Override
    public List<IamRoleAssignmentEntity> scanAssignments(long roleId, BigInteger after,
            List<BigInteger> sourceIds, List<BigInteger> assignmentIds, ObjectScope scope, int limit) {
        return assignmentStore.roleApplicationRows(roleId, after, sourceIds, assignmentIds, scope, limit);
    }

    /** {@inheritDoc} */
    @Override
    public Page<IamRoleAssignmentEntity> pageAssignments(long roleId, int page, int size, ObjectScope scope) {
        return assignmentStore.pageRoleApplication(roleId, page, size, scope);
    }

    /** {@inheritDoc} */
    @Override
    public List<RoleRevisionDistribution> distribution(long roleId, ObjectScope scope) {
        return assignmentStore.applicationDistribution(roleId, scope);
    }

    /** {@inheritDoc} */
    @Override
    public IamRoleAssignmentEntity findAssignment(long assignmentId) {
        return assignmentStore.find(AuthorizationDomain.PLATFORM, null, assignmentId);
    }

    /** {@inheritDoc} */
    @Override
    public AssignmentRecord snapshot(IamAdmission admission, IamRoleAssignmentEntity assignment) {
        requirePlatform(admission);
        return assignments.applicationSnapshot(admission, assignment);
    }

    /** {@inheritDoc} */
    @Override
    public AssignmentUpgradeResult preview(IamAdmission admission, AssignmentUpgradeInput input) {
        requirePlatform(admission);
        return assignments.previewApplication(admission, input);
    }

    /** {@inheritDoc} */
    @Override
    public void apply(IamAdmission admission, AssignmentUpgradeInput input) {
        requirePlatform(admission);
        assignments.applyApplication(admission, input);
    }

    private static void requirePlatform(IamAdmission admission) {
        var context = admission.actor().context();
        if (context.domain() != AuthorizationDomain.PLATFORM || context.tenantId() != null)
            throw new BizException(IamReasonCode.ACTION_DENIED);
    }
}
