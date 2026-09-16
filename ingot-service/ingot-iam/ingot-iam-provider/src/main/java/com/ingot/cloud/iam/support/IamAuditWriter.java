package com.ingot.cloud.iam.support;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.persistence.entity.IamAuthorizationAuditEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * <p>与业务写操作同事务保存脱敏审计事实，不记录凭证或联系方式原值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class IamAuditWriter {
    private static final String TRACE_ID = "traceId";
    private static final int TRACE_LIMIT = 128;
    private final IamAuthorizationAuditMapper mapper;

    /**
     * 写入一条审计。调用方必须已处于业务事务中。
     *
     * @param actor 可信当前身份
     * @param auditId 服务器发号
     * @param targetType 资源编码
     * @param targetId 目标 ID
     * @param changeType 变更类型
     * @param before 变更前安全差异
     * @param after 变更后安全差异
     * @param revisions 相关版本
     */
    public void write(AuthorizationContext actor, long auditId, String targetType, String targetId,
                      AuditChangeType changeType, Map<AuditField, ?> before, Map<AuditField, ?> after,
                      Map<String, String> revisions) {
        write(actor, auditId, targetType, targetId, changeType, before, after, revisions, null, null);
    }

    /**
     * 写入带委派或授权关联的审计，并从当前链路读取追踪标识。
     *
     * @param actor 可信当前身份
     * @param auditId 服务器发号
     * @param targetType 资源编码
     * @param targetId 目标 ID
     * @param changeType 变更类型
     * @param before 变更前安全差异
     * @param after 变更后安全差异
     * @param revisions 相关版本
     * @param delegationId 来源委派 ID，可空
     * @param assignmentId 相关分配 ID，可空
     */
    public void write(AuthorizationContext actor, long auditId, String targetType, String targetId,
                      AuditChangeType changeType, Map<AuditField, ?> before, Map<AuditField, ?> after,
                      Map<String, String> revisions, String delegationId, String assignmentId) {
        var row = new IamAuthorizationAuditEntity();
        row.setId(BigInteger.valueOf(auditId));
        row.setEventId(UUID.randomUUID().toString());
        row.setActorAccountId(new BigInteger(actor.accountId()));
        row.setActorMemberId(new BigInteger(actor.memberId()));
        row.setDomain(actor.domain());
        row.setTenantId(actor.tenantId() == null ? null : new BigInteger(actor.tenantId()));
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setChangeType(changeType);
        row.setSafeBefore(IamJson.object(before == null ? Map.of() : before));
        row.setSafeAfter(IamJson.object(after == null ? Map.of() : after));
        row.setRevisions(IamJson.object(revisions == null ? Map.of() : revisions));
        row.setDelegationId(optionalId(delegationId));
        row.setAssignmentId(optionalId(assignmentId));
        row.setTraceId(currentTraceId());
        row.setOccurredAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        mapper.insert(row);
    }

    private static BigInteger optionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return BigInteger.valueOf(IamIds.require(value));
    }

    private static String currentTraceId() {
        String trace = MDC.get(TRACE_ID);
        if (trace == null || trace.isBlank()) {
            return null;
        }
        return trace.length() <= TRACE_LIMIT ? trace : trace.substring(0, TRACE_LIMIT);
    }
}
