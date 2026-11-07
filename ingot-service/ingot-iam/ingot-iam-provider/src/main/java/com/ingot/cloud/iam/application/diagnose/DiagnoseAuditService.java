package com.ingot.cloud.iam.application.diagnose;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.infrastructure.persistence.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.authorization.candidate.PlatformAuthorizationCandidateQueryService;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.cloud.iam.domain.shared.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.diagnosis.model.Decision;
import com.ingot.cloud.iam.application.diagnosis.model.DecisionSource;
import com.ingot.cloud.iam.application.diagnosis.model.DiagnoseInput;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>提供受限授权诊断，不执行目标操作也不返回凭证明文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DiagnoseAuditService {
    private static final String SCOPE_SUMMARY = "不包含越界对象明细";
    private final IamAccess access;
    private final AuthorizationEvaluator evaluator;
    private final AuthorizationEvaluationRepository evaluations;
    private final ResourceScopeAccessService scopes;
    private final FieldAccessEvaluator fieldAccess;
    private final IamPlatformMemberMapper platformMembers;
    private final IamTenantMemberMapper tenantMembers;
    private final IamApplicationMapper applications;
    private final PlatformAuthorizationCandidateQueryService platformCandidates;
    private final TenantDiagnoseCandidates tenantCandidates;

    /**
     * 对当前域目标身份做只读诊断。
     *
     * @param domain 接口管理域
     * @param input 诊断目标
     * @return 受限解释
     */
    public Decision diagnose(AuthorizationDomain domain, DiagnoseInput input) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE : IamAction.TENANT_AUTHORIZATION_DIAGNOSE);
        AuthorizationContext target = resolveMember(domain, actor, input);
        String memberId = target.memberId();
        if (!memberId.equals(actor.context().memberId())) {
            IamAction memberRead = domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ;
            scopes.requireVisibleMember(actor.context(), memberRead, IamIds.require(memberId));
        }
        IamApplicationEntity application = applications.selectOne(Wrappers.<IamApplicationEntity>lambdaQuery()
                .eq(IamApplicationEntity::getId, id(IamIds.require(input.applicationId())))
                .eq(IamApplicationEntity::getDomain, domain));
        if (application == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        List<AuthorizationEvalRows.Action> actionRows = evaluations.listActions(id(IamIds.require(input.actionId())));
        if (actionRows.size() != 1 || !application.getId().equals(actionRows.getFirst().applicationId())) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        AuthorizationEvalRows.Action action = actionRows.getFirst();
        ResourceKey resource = null;
        if (input.targetId() != null && !input.targetId().isBlank()) {
            resource = evaluations.resourceKey(id(IamIds.require(input.actionId())));
            requireOperatorTarget(domain, input, resource);
        }
        AuthorizationView view = evaluator.evaluate(target);
        IamReasonCode denied = denial(domain, actor, target, application, action, resource, view, input.targetId());
        boolean allowed = denied == null;
        AuthorizationView operatorView = evaluator.evaluate(actor.context());
        List<DecisionSource> sources = List.of();
        if (discloseSources(domain, actor, memberId, operatorView)) {
            sources = evaluator.decisionSources(target, input.actionId());
        } else if (domain == AuthorizationDomain.PLATFORM
                && operatorView.actionCodes().contains(IamAction.PLATFORM_ASSIGNMENT_READ.getCode())) {
            var owned = evaluations.effectiveDelegations(actor.context()).stream()
                    .map(row -> row.getId().toString()).collect(java.util.stream.Collectors.toSet());
            sources = evaluator.decisionSources(target, input.actionId()).stream()
                    .filter(source -> source.delegationId() != null && owned.contains(source.delegationId())).toList();
        }
        return new Decision(allowed, denied, allowed ? "当前计算允许该操作" : denied.getText(), sources, SCOPE_SUMMARY,
                discloseFields(actor, resource, input.targetId()), view.version(), view.expiresAt());
    }

    private AuthorizationContext resolveMember(AuthorizationDomain domain, ActiveIdentity actor, DiagnoseInput input) {
        var current = actor.context();
        if (current.memberId().equals(input.memberId()) || current.accountId().equals(input.accountId())) {
            return current;
        }
        boolean byMember = input.memberId() != null && !input.memberId().isBlank();
        BigInteger value = id(IamIds.require(byMember ? input.memberId() : input.accountId()));
        if (domain == AuthorizationDomain.PLATFORM) {
            var query = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                    .eq(byMember ? IamPlatformMemberEntity::getId : IamPlatformMemberEntity::getAccountId, value)
                    .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED);
            var member = platformMembers.selectOne(query);
            if (member == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return new AuthorizationContext(domain, null, member.getAccountId().toString(), member.getId().toString());
        }
        var query = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, id(IamIds.require(current.tenantId())))
                .eq(byMember ? IamTenantMemberEntity::getId : IamTenantMemberEntity::getAccountId, value)
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED);
        var member = tenantMembers.selectOne(query);
        if (member == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return new AuthorizationContext(domain, current.tenantId(), member.getAccountId().toString(),
                member.getId().toString());
    }

    private IamReasonCode denial(AuthorizationDomain domain, ActiveIdentity actor, AuthorizationContext target,
                                 IamApplicationEntity application, AuthorizationEvalRows.Action action, ResourceKey resource,
                                 AuthorizationView view, String targetId) {
        if (!Boolean.TRUE.equals(application.getEnabled()) || !Boolean.TRUE.equals(action.appEnabled())) {
            return IamReasonCode.APPLICATION_UNAVAILABLE;
        }
        if (domain == AuthorizationDomain.TENANT && !(view.tenantOwner() && view.actionCodes().contains(action.code()))) {
            AuthorizationEvalRows.Entitlement entitlement = evaluations.entitlement(
                    id(IamIds.require(actor.context().tenantId())), application.getId(),
                    id(IamIds.require(target.memberId())));
            if (entitlement == null || entitlement.hits() <= 0) {
                return IamReasonCode.APPLICATION_UNAVAILABLE;
            }
        }
        if (!Boolean.TRUE.equals(action.enabled()) || !view.actionCodes().contains(action.code())) {
            return IamReasonCode.ACTION_DENIED;
        }
        if (targetId != null && !targetId.isBlank()
                && !scopes.targetAllowed(target, view, resource, action.code(), targetId)) {
            return IamReasonCode.DATA_SCOPE_DENIED;
        }
        return null;
    }

    private void requireOperatorTarget(AuthorizationDomain domain, DiagnoseInput input, ResourceKey resource) {
        if (resource == null || resource.domain() != domain) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        var ids = List.of(input.targetId());
        var candidates = domain == AuthorizationDomain.PLATFORM
                ? platformCandidates.diagnose(AuthorizationCandidateKind.OBJECT, input.actionId(),
                        input.applicationId(), null, ids, 1, 1)
                : tenantCandidates.list(AuthorizationCandidateKind.OBJECT, input.applicationId(), input.actionId(),
                        null, ids, 1, 1);
        if (!candidates.supported() || candidates.items().stream().noneMatch(item -> input.targetId().equals(item.id()))) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private Map<String, FieldAccess> discloseFields(ActiveIdentity actor, ResourceKey resource, String targetId) {
        if (targetId == null || targetId.isBlank() || !BuiltinResourceProviders.TENANT_MEMBER_RESOURCE.equals(resource)) {
            return Map.of();
        }
        return fieldAccess.memberAccess(IamIds.require(actor.context().tenantId()),
                IamIds.require(actor.context().memberId()), IamIds.require(targetId), resource);
    }

    private static boolean discloseSources(AuthorizationDomain domain, ActiveIdentity actor, String memberId,
                                           AuthorizationView operatorView) {
        if (memberId.equals(actor.context().memberId())) {
            return true;
        }
        IamAction assignmentRead = domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_ASSIGNMENT_READ : IamAction.TENANT_ASSIGNMENT_READ;
        return operatorView.governedCodes().contains(assignmentRead.getCode());
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }
}
