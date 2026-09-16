package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamAuthorizationAuditEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditActor;
import com.ingot.framework.commons.model.iam.AuditEntry;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuditTarget;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DecisionSource;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>提供受限授权诊断与脱敏审计列表，不执行目标操作也不返回凭证明文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DiagnoseAuditService {
    private static final TypeReference<Map<String, String>> TEXT_MAP = new TypeReference<>() {
    };
    private static final String SCOPE_SUMMARY = "不包含越界对象明细";
    private final IamAccess access;
    private final AuthorizationEvaluator evaluator;
    private final AuthorizationEvaluationRepository evaluations;
    private final ResourceAccess scopes;
    private final FieldAccessEvaluator fieldAccess;
    private final IamAuthorizationAuditMapper audits;
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
        scopes.requireVisibleMember(actor.context(), memberRead, IamIds.require(memberId));
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
        List<DecisionSource> sources = discloseSources(domain, actor, memberId, operatorView)
                ? List.of(new DecisionSource(null, null, null, "已按当前身份展开有效授权"))
                : List.of();
        return new Decision(allowed, denied, allowed ? "当前计算允许该操作" : denied.getText(), sources, SCOPE_SUMMARY,
                discloseFields(actor, input.targetId()), view.version(), view.expiresAt());
    }

    /**
     * 分页列出当前域脱敏审计。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 审计页
     */
    public PageResponse<ResourceDetail<AuditEntry>> listAudits(AuthorizationDomain domain, int page, int pageSize) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_AUDIT_READ : IamAction.TENANT_AUDIT_READ);
        IamPages.require(page, pageSize);
        var wrapper = Wrappers.<IamAuthorizationAuditEntity>lambdaQuery()
                .select(IamAuthorizationAuditEntity::getId, IamAuthorizationAuditEntity::getActorAccountId,
                        IamAuthorizationAuditEntity::getActorMemberId, IamAuthorizationAuditEntity::getDomain,
                        IamAuthorizationAuditEntity::getTenantId, IamAuthorizationAuditEntity::getTargetType,
                        IamAuthorizationAuditEntity::getTargetId, IamAuthorizationAuditEntity::getChangeType,
                        IamAuthorizationAuditEntity::getSafeBefore, IamAuthorizationAuditEntity::getSafeAfter,
                        IamAuthorizationAuditEntity::getRevisions, IamAuthorizationAuditEntity::getDelegationId,
                        IamAuthorizationAuditEntity::getAssignmentId, IamAuthorizationAuditEntity::getTraceId,
                        IamAuthorizationAuditEntity::getOccurredAt)
                .eq(IamAuthorizationAuditEntity::getDomain, domain);
        if (domain == AuthorizationDomain.PLATFORM) {
            wrapper.isNull(IamAuthorizationAuditEntity::getTenantId);
        } else {
            wrapper.eq(IamAuthorizationAuditEntity::getTenantId, id(IamIds.require(actor.context().tenantId())));
        }
        wrapper.orderByDesc(IamAuthorizationAuditEntity::getOccurredAt, IamAuthorizationAuditEntity::getId);
        Page<IamAuthorizationAuditEntity> result = audits.selectPage(new Page<>(page, pageSize), wrapper);
        Map<BigInteger, String> names = actorNames(domain, actor, result.getRecords());
        FieldPolicySnapshot snapshot = domain == AuthorizationDomain.TENANT && !names.isEmpty()
                ? fieldAccess.snapshot(IamIds.require(actor.context().tenantId()), PolicyScenario.MANAGEMENT) : null;
        long viewerId = IamIds.require(actor.context().memberId());
        List<ResourceDetail<AuditEntry>> items = result.getRecords().stream()
                .map(row -> IamDetails.of(entry(row, names, snapshot, viewerId), row.getId().toString()))
                .toList();
        return IamPages.details(items, result.getTotal(), page, pageSize);
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
        if (domain == AuthorizationDomain.TENANT) {
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
        return operatorView.actionCodes().contains(assignmentRead.getCode());
    }

    private static String resourceOf(String actionCode) {
        int first = actionCode.indexOf(':');
        int last = actionCode.lastIndexOf(':');
        if (first < 0 || last <= first) {
            return "";
        }
        return actionCode.substring(first + 1, last);
    }

    private AuditEntry entry(IamAuthorizationAuditEntity row, Map<BigInteger, String> names,
                             FieldPolicySnapshot snapshot, long viewerId) {
        AuthorizationDomain domain = row.getDomain();
        String tenantId = row.getTenantId() == null ? null : row.getTenantId().toString();
        Instant occurred = row.getOccurredAt().toInstant(ZoneOffset.UTC);
        String rawName = names.get(row.getActorMemberId());
        String displayName = rawName == null ? null
                : snapshot == null ? rawName
                : FieldProjection.project(rawName, fieldAccess.access(snapshot, viewerId,
                        row.getActorMemberId().longValueExact(), MemberFieldKey.VALUE_DISPLAY_NAME).visibility());
        return new AuditEntry(row.getId().toString(),
                new AuditActor(row.getActorAccountId().toString(), row.getActorMemberId().toString(), displayName),
                new AuthorizationContext(domain, tenantId, row.getActorAccountId().toString(),
                        row.getActorMemberId().toString()),
                new AuditTarget(row.getTargetType(), row.getTargetId()),
                row.getChangeType(),
                fields(row.getSafeBefore()), fields(row.getSafeAfter()),
                revisions(row.getRevisions()),
                text(row.getDelegationId()), text(row.getAssignmentId()), occurred, row.getTraceId());
    }

    private Map<BigInteger, String> actorNames(AuthorizationDomain domain, ActiveIdentity actor,
                                               List<IamAuthorizationAuditEntity> rows) {
        LinkedHashSet<BigInteger> ids = new LinkedHashSet<>();
        for (IamAuthorizationAuditEntity row : rows) {
            if (row.getActorMemberId() != null) {
                ids.add(row.getActorMemberId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        IamAction memberRead = domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ;
        AuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(actor.context());
        if (!view.actionCodes().contains(memberRead.getCode())) {
            return Map.of();
        }
        ObjectScope scope = scopes.memberRead(actor.context(), memberRead);
        if (scope.coversNone()) {
            return Map.of();
        }
        Map<BigInteger, String> names = new LinkedHashMap<>();
        if (domain == AuthorizationDomain.PLATFORM) {
            var wrapper = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                    .select(IamPlatformMemberEntity::getId, IamPlatformMemberEntity::getDisplayName)
                    .in(IamPlatformMemberEntity::getId, ids)
                    .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED);
            ObjectScopeSql.restrictPlatformMembers(wrapper, scope);
            for (IamPlatformMemberEntity row : platformMembers.selectList(wrapper)) {
                names.put(row.getId(), row.getDisplayName());
            }
            return names;
        }
        BigInteger tenant = id(IamIds.require(actor.context().tenantId()));
        var wrapper = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .select(IamTenantMemberEntity::getId, IamTenantMemberEntity::getDisplayName)
                .eq(IamTenantMemberEntity::getTenantId, tenant)
                .in(IamTenantMemberEntity::getId, ids)
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED);
        ObjectScopeSql.restrictTenantMembers(wrapper, scope, tenant);
        for (IamTenantMemberEntity row : tenantMembers.selectList(wrapper)) {
            names.put(row.getId(), row.getDisplayName());
        }
        return names;
    }

    private static String text(BigInteger value) {
        return value == null ? null : value.toString();
    }

    private static Map<AuditField, String> fields(String json) {
        Map<String, String> raw = IamJson.read(json, TEXT_MAP);
        Map<AuditField, String> result = new LinkedHashMap<>();
        if (raw == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            try {
                result.put(AuditField.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ignored) {
                // 非白名单字段已由写入路径过滤，读取时忽略未知键。
            }
        }
        return result;
    }

    private static Map<String, String> revisions(String json) {
        Map<String, String> raw = IamJson.read(json, TEXT_MAP);
        return raw == null ? Map.of() : raw;
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }
}
