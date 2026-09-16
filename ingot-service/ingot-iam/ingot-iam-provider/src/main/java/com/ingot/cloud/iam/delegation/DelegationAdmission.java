package com.ingot.cloud.iam.delegation;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.ScopeBinder;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.DelegationRecipientRepository;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.SubjectRef;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>核对一条委派派生的授权是否成立，供授权写入与角色升级共用同一套口径。</p>
 *
 * <p>校验委派存在且有效、角色版本在白名单内、接收者仍被人群覆盖、分配期限不超过委派上限，
 * 并逐操作核对分配范围被委派范围上限覆盖。委派未给出某个操作的上限时按失败关闭拒绝，
 * 避免落一条运行时形同废纸的授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DelegationAdmission {
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private final AssignmentRepository assignments;
    private final DelegationRecipientRepository recipients;

    /**
     * 校验一条以委派为来源的授权。
     *
     * @param request 待校验的授权事实
     * @return 校验问题；全部通过时为空
     */
    public List<ValidationIssue> check(Request request) {
        List<ValidationIssue> errors = new ArrayList<>();
        IamDelegationGrantEntity delegation = assignments.findDelegation(request.domain(), request.tenantId(),
                request.delegationId());
        if (delegation == null || delegation.getStatus() != GrantStatus.ACTIVE) {
            errors.add(new ValidationIssue("delegationGrantId", IamReasonCode.OBJECT_NOT_FOUND, "委派不存在或已撤销"));
            return errors;
        }
        if (request.ownerMemberId() != null && !owned(delegation, request.ownerMemberId())) {
            errors.add(new ValidationIssue("delegationGrantId", IamReasonCode.ACTION_DENIED, "不得借用他人的委派"));
        }
        Instant now = Instant.now();
        Instant validFrom = instant(delegation.getValidFrom());
        Instant validUntil = instant(delegation.getValidUntil());
        if (validFrom != null && now.isBefore(validFrom) || validUntil != null && !now.isBefore(validUntil)) {
            errors.add(new ValidationIssue("delegationGrantId", IamReasonCode.ACTION_DENIED, "委派不在有效期内"));
        }
        errors.addAll(duration(delegation, request));
        if (!assignments.delegationAllowsRevision(request.delegationId(), request.revisionId())) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.ACTION_DENIED, "委派不允许该角色版本"));
        } else {
            errors.addAll(ceilings(request));
        }
        if (!reaches(request)) {
            errors.add(new ValidationIssue("subject", IamReasonCode.ACTION_DENIED, "接收者不在委派人群内"));
        }
        return List.copyOf(errors);
    }

    private List<ValidationIssue> duration(IamDelegationGrantEntity delegation, Request request) {
        Instant from = request.validFrom() == null ? Instant.now() : request.validFrom();
        if (request.validUntil() == null) {
            return List.of(new ValidationIssue("validUntil", IamReasonCode.INVALID_ARGUMENT,
                    "来源委派的分配必须有结束时间"));
        }
        Duration max = Duration.ofSeconds(
                delegation.getMaxAssignmentDurationSeconds() == null ? 0 : delegation.getMaxAssignmentDurationSeconds(),
                delegation.getMaxAssignmentDurationNanos() == null ? 0 : delegation.getMaxAssignmentDurationNanos());
        if (Duration.between(from, request.validUntil()).compareTo(max) > 0) {
            return List.of(new ValidationIssue("validUntil", IamReasonCode.INVALID_ARGUMENT, "分配期限超过委派上限"));
        }
        return List.of();
    }

    private List<ValidationIssue> ceilings(Request request) {
        Map<String, ActionScopeCeiling> ceilings = load(request.delegationId());
        for (ActionGrant grant : request.grants()) {
            ActionScopeCeiling ceiling = ceilings.get(grant.actionId());
            if (ceiling == null) {
                return List.of(new ValidationIssue("delegationGrantId", IamReasonCode.ACTION_DENIED,
                        "委派未给出该角色全部操作的范围上限"));
            }
            if (!ScopeBinder.covers(ScopeBinder.bind(ceiling), ScopeBinder.bind(grant, request.bindings()))) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.ACTION_DENIED, "分配范围超出委派上限"));
            }
        }
        return List.of();
    }

    private Map<String, ActionScopeCeiling> load(long delegationId) {
        Map<String, ActionScopeCeiling> ceilings = new LinkedHashMap<>();
        for (AuthorizationEvalRows.Ceiling row : assignments.loadCeilings(delegationId)) {
            String actionId = row.actionId().toString();
            List<ScopeExpression> scopes = IamJson.read(row.scopes(), SCOPES);
            Map<String, ScopeBinding> bindings = IamJson.read(row.scopeBindings(), BINDINGS);
            ceilings.put(actionId, new ActionScopeCeiling(actionId, scopes == null ? List.of() : scopes,
                    bindings == null ? Map.of() : bindings));
        }
        return ceilings;
    }

    private boolean reaches(Request request) {
        SubjectRef subject = request.subject();
        if (subject.type() == SubjectType.MEMBER) {
            return recipients.reaches(request.domain(), request.tenantId(), request.delegationId(),
                    IamIds.require(subject.id()));
        }
        List<BigInteger> members = request.domain() == AuthorizationDomain.PLATFORM
                ? assignments.platformGroupMemberIds(IamIds.require(subject.id()))
                : assignments.tenantGroupMemberIds(request.tenantId(), IamIds.require(subject.id()));
        return !members.isEmpty() && members.stream().allMatch(memberId -> recipients.reaches(request.domain(),
                request.tenantId(), request.delegationId(), memberId.longValue()));
    }

    private static boolean owned(IamDelegationGrantEntity delegation, long memberId) {
        BigInteger administrator = delegation.getPlatformAdministratorId() == null
                ? delegation.getTenantAdministratorId() : delegation.getPlatformAdministratorId();
        return administrator != null && administrator.longValue() == memberId;
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /**
     * <p>一条待校验的委派派生授权事实。</p>
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域为空
     * @param delegationId 声明的来源委派 ID
     * @param ownerMemberId 必须持有该委派的成员 ID；为空表示本次不校验持有者
     * @param revisionId 分配指向的角色版本 ID
     * @param grants 该版本合成后的操作授权
     * @param bindings 分配上的范围参数绑定
     * @param subject 接收主体
     * @param validFrom 分配生效时间，为空按当前时间
     * @param validUntil 分配失效时间，为空表示未给出结束时间
     * @author jy
     * @since 1.0.0
     */
    public record Request(AuthorizationDomain domain, Long tenantId, long delegationId, Long ownerMemberId,
                          long revisionId, List<ActionGrant> grants, Map<String, ScopeBinding> bindings,
                          SubjectRef subject, Instant validFrom, Instant validUntil) {
    }
}
