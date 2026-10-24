package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.assignment.PlatformScopeObjectResource;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DecisionSource;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PolicyScenario;
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
    private final ResourceAccess scopes;
    private final FieldAccessEvaluator fieldAccess;
    private final IamPlatformMemberMapper platformMembers;
    private final IamTenantMemberMapper tenantMembers;
    private final IamApplicationMapper applications;

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
        String memberId = resolveMember(domain, actor, input);
        IamAction memberRead = domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ;
        if (domain == AuthorizationDomain.PLATFORM) {
            if (!scopes.targetAllowed(actor.context(), evaluator.evaluate(actor.context()),
                    IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE.getCode(), memberId)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        } else {
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
        AuthorizationContext target = new AuthorizationContext(domain,
                domain == AuthorizationDomain.TENANT ? actor.context().tenantId() : null,
                actor.context().accountId(), memberId);
        requireOperatorTarget(actor, memberRead, input.targetId(), action.code());
        AuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(target);
        IamReasonCode denied = denial(domain, actor, target, application, action, view, input.targetId());
        boolean allowed = denied == null;
        AuthorizationEvaluator.AuthorizationView operatorView = evaluator.evaluate(actor.context());
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
                discloseFields(actor, input.targetId()), view.version(), view.expiresAt());
    }

    private String resolveMember(AuthorizationDomain domain, ActiveIdentity actor, DiagnoseInput input) {
        if (input.memberId() != null && !input.memberId().isBlank()) {
            long memberId = IamIds.require(input.memberId());
            long count = domain == AuthorizationDomain.PLATFORM
                    ? platformMembers.selectCount(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                    .eq(IamPlatformMemberEntity::getId, id(memberId))
                    .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED))
                    : tenantMembers.selectCount(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                    .eq(IamTenantMemberEntity::getTenantId, id(IamIds.require(actor.context().tenantId())))
                    .eq(IamTenantMemberEntity::getId, id(memberId))
                    .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED));
            if (count == 0) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return input.memberId();
        }
        long accountId = IamIds.require(input.accountId());
        List<String> members = domain == AuthorizationDomain.PLATFORM
                ? platformMembers.selectList(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                        .select(IamPlatformMemberEntity::getId)
                        .eq(IamPlatformMemberEntity::getAccountId, id(accountId))
                        .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)).stream()
                .map(row -> row.getId().toString()).toList()
                : tenantMembers.selectList(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                        .select(IamTenantMemberEntity::getId)
                        .eq(IamTenantMemberEntity::getTenantId, id(IamIds.require(actor.context().tenantId())))
                        .eq(IamTenantMemberEntity::getAccountId, id(accountId))
                        .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)).stream()
                .map(row -> row.getId().toString()).toList();
        if (members.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return members.getFirst();
    }

    private IamReasonCode denial(AuthorizationDomain domain, ActiveIdentity actor, AuthorizationContext target,
                                 IamApplicationEntity application, AuthorizationEvalRows.Action action,
                                 AuthorizationEvaluator.AuthorizationView view, String targetId) {
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
                && !scopes.targetAllowed(target, view, action.code(), targetId)) {
            return IamReasonCode.DATA_SCOPE_DENIED;
        }
        return null;
    }

    private void requireOperatorTarget(ActiveIdentity actor, IamAction memberRead, String targetId, String actionCode) {
        if (targetId == null || targetId.isBlank()) {
            return;
        }
        if (actor.context().domain() == AuthorizationDomain.PLATFORM) {
            PlatformScopeObjectResource resource = PlatformScopeObjectResource.find(resourceOf(actionCode));
            if (resource == null || !scopes.targetAllowed(actor.context(), evaluator.evaluate(actor.context()),
                    resource.getReadAction().getCode(), targetId)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return;
        }
        String resource = resourceOf(actionCode);
        if ("member".equals(resource)) {
            scopes.requireVisibleMember(actor.context(), memberRead, IamIds.require(targetId));
            return;
        }
        if ("department".equals(resource)) {
            if (actor.context().domain() != AuthorizationDomain.TENANT) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            AuthorizationEvaluator.AuthorizationView operatorView = evaluator.evaluate(actor.context());
            if (!scopes.departmentAllowed(actor.context(), operatorView, IamAction.TENANT_DEPARTMENT_READ,
                    IamIds.require(targetId))) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return;
        }
        AuthorizationEvaluator.AuthorizationView operatorView = evaluator.evaluate(actor.context());
        if (!scopes.targetAllowed(actor.context(), operatorView, actionCode, targetId)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private Map<String, FieldAccess> discloseFields(ActiveIdentity actor, String targetId) {
        if (targetId == null || targetId.isBlank() || actor.context().domain() != AuthorizationDomain.TENANT) {
            return Map.of();
        }
        return fieldAccess.memberAccess(IamIds.require(actor.context().tenantId()),
                IamIds.require(actor.context().memberId()), IamIds.require(targetId), PolicyScenario.MANAGEMENT);
    }

    private static boolean discloseSources(AuthorizationDomain domain, ActiveIdentity actor, String memberId,
                                           AuthorizationEvaluator.AuthorizationView operatorView) {
        if (memberId.equals(actor.context().memberId())) {
            return true;
        }
        IamAction assignmentRead = domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_ASSIGNMENT_READ : IamAction.TENANT_ASSIGNMENT_READ;
        return operatorView.governedCodes().contains(assignmentRead.getCode());
    }

    private static String resourceOf(String actionCode) {
        int first = actionCode.indexOf(':');
        int last = actionCode.lastIndexOf(':');
        if (first < 0 || last <= first) {
            return "";
        }
        return actionCode.substring(first + 1, last);
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }
}
