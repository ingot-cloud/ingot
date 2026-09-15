package com.ingot.cloud.iam.support;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.cloud.iam.persistence.entity.IamAuthorizationAuditEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import lombok.RequiredArgsConstructor;
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
        row.setOccurredAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        mapper.insert(row);
    }
}
