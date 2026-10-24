package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.entity.IamAuthorizationAuditEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditValues;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditActor;
import com.ingot.framework.commons.model.iam.AuditCursorPage;
import com.ingot.framework.commons.model.iam.AuditEntry;
import com.ingot.framework.commons.model.iam.AuditReference;
import com.ingot.framework.commons.model.iam.AuditReferenceState;
import com.ingot.framework.commons.model.iam.AuditReferenceType;
import com.ingot.framework.commons.model.iam.AuditTarget;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按 {@code occurred_at DESC, id DESC} 键集游标列出当前域脱敏审计，不计算总数，并附带页级对象名称。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthorizationAuditService {
    /** 默认每页条数。 */
    public static final int DEFAULT_LIMIT = 20;
    /** 最大每页条数。 */
    public static final int MAX_LIMIT = 100;
    private final IamAccess access;
    private final AuthorizationEvaluator evaluator;
    private final IamAuthorizationAuditMapper audits;
    private final AuditReferenceResolver resolver;

    /**
     * 在可信域与租户边界内按筛选条件和游标取一页审计；筛选、游标与排序在同一条 SQL 中生效。
     *
     * @param domain 接口管理域
     * @param limit 每页条数，1–{@value #MAX_LIMIT}
     * @param cursor 上一页的 {@code nextCursor}，空表示第一页
     * @param query 已校验的筛选条件
     * @return 游标页
     */
    public AuditCursorPage list(AuthorizationDomain domain, int limit, String cursor, AuthorizationAuditQuery query) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_AUDIT_READ : IamAction.TENANT_AUDIT_READ);
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        AuditCursor after = AuditCursor.decode(cursor);
        LambdaQueryWrapper<IamAuthorizationAuditEntity> wrapper = condition(domain, actor, query);
        if (after != null) {
            wrapper.and(position -> position.lt(IamAuthorizationAuditEntity::getOccurredAt, after.occurredAt())
                    .or(tie -> tie.eq(IamAuthorizationAuditEntity::getOccurredAt, after.occurredAt())
                            .lt(IamAuthorizationAuditEntity::getId, after.id())));
        }
        wrapper.orderByDesc(IamAuthorizationAuditEntity::getOccurredAt, IamAuthorizationAuditEntity::getId);
        List<IamAuthorizationAuditEntity> rows = audits.selectPage(new Page<>(1, limit + 1L, false), wrapper)
                .getRecords();
        boolean hasMore = rows.size() > limit;
        List<IamAuthorizationAuditEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<AuditEntry> entries = page.stream().map(AuthorizationAuditService::entry).toList();
        AuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(actor.context());
        Map<String, AuditReference> references = resolver.resolve(actor, view, entries);
        List<ResourceDetail<AuditEntry>> items = entries.stream()
                .map(entry -> IamDetails.of(withActorName(entry, references), entry.id()))
                .toList();
        String next = hasMore ? new AuditCursor(page.getLast().getOccurredAt(), page.getLast().getId()).encode() : null;
        return new AuditCursorPage(items, next, hasMore, references);
    }

    private static LambdaQueryWrapper<IamAuthorizationAuditEntity> condition(AuthorizationDomain domain,
                                                                            ActiveIdentity actor,
                                                                            AuthorizationAuditQuery query) {
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
            wrapper.eq(IamAuthorizationAuditEntity::getTenantId,
                    BigInteger.valueOf(IamIds.require(actor.context().tenantId())));
        }
        if (query.actorMemberId() != null) {
            wrapper.eq(IamAuthorizationAuditEntity::getActorMemberId,
                    BigInteger.valueOf(IamIds.require(query.actorMemberId())));
        }
        if (query.targetType() != null) {
            wrapper.eq(IamAuthorizationAuditEntity::getTargetType, query.targetType());
        }
        if (query.targetId() != null) {
            wrapper.eq(IamAuthorizationAuditEntity::getTargetId, query.targetId());
        }
        if (query.changeType() != null) {
            wrapper.eq(IamAuthorizationAuditEntity::getChangeType, query.changeType());
        }
        if (query.from() != null) {
            wrapper.ge(IamAuthorizationAuditEntity::getOccurredAt, LocalDateTime.ofInstant(query.from(), ZoneOffset.UTC));
        }
        if (query.until() != null) {
            wrapper.lt(IamAuthorizationAuditEntity::getOccurredAt, LocalDateTime.ofInstant(query.until(), ZoneOffset.UTC));
        }
        return wrapper;
    }

    private static AuditEntry entry(IamAuthorizationAuditEntity row) {
        String id = row.getId().toString();
        String tenantId = row.getTenantId() == null ? null : row.getTenantId().toString();
        return new AuditEntry(id,
                new AuditActor(row.getActorAccountId().toString(), row.getActorMemberId().toString(), null),
                new AuthorizationContext(row.getDomain(), tenantId, row.getActorAccountId().toString(),
                        row.getActorMemberId().toString()),
                new AuditTarget(row.getTargetType(), row.getTargetId()),
                row.getChangeType(),
                IamAuditValues.readFields(id, IamAuditValues.BEFORE_COLUMN, row.getSafeBefore()),
                IamAuditValues.readFields(id, IamAuditValues.AFTER_COLUMN, row.getSafeAfter()),
                IamAuditValues.readRevisions(id, row.getRevisions()),
                text(row.getDelegationId()), text(row.getAssignmentId()),
                row.getOccurredAt().toInstant(ZoneOffset.UTC), row.getTraceId());
    }

    private static AuditEntry withActorName(AuditEntry entry, Map<String, AuditReference> references) {
        AuditReference actor = references.get(AuditReferenceType.MEMBER.key(entry.actor().memberId()));
        if (actor == null || actor.state() != AuditReferenceState.VISIBLE) {
            return entry;
        }
        return new AuditEntry(entry.id(), new AuditActor(entry.actor().accountId(), entry.actor().memberId(),
                actor.name()), entry.context(), entry.target(), entry.changeType(), entry.before(), entry.after(),
                entry.revisions(), entry.delegationId(), entry.assignmentId(), entry.timestamp(), entry.traceId());
    }

    private static String text(BigInteger value) {
        return value == null ? null : value.toString();
    }
}
