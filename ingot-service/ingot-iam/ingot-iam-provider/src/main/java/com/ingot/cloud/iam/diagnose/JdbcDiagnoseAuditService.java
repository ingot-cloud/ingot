package com.ingot.cloud.iam.diagnose;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.JdbcAuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditActor;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditEntry;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuditTarget;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DecisionSource;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>提供受限授权诊断与脱敏审计列表，不执行目标操作也不返回凭证明文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class JdbcDiagnoseAuditService {
    private static final TypeReference<Map<String, String>> TEXT_MAP = new TypeReference<>() {
    };
    private final IamAccess access;
    private final JdbcAuthorizationEvaluator evaluator;
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * 绑定身份、求值器与审计表。
     *
     * @param access 当前身份
     * @param evaluator 真实求值引擎
     * @param dataSource IAM 目标库
     */
    public JdbcDiagnoseAuditService(IamAccess access, JdbcAuthorizationEvaluator evaluator, DataSource dataSource) {
        this.access = access;
        this.evaluator = evaluator;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

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
        AuthorizationContext target = new AuthorizationContext(domain,
                domain == AuthorizationDomain.TENANT ? actor.context().tenantId() : null,
                actor.context().accountId(), memberId);
        JdbcAuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(target);
        String actionCode = actionCode(IamIds.require(input.actionId()));
        boolean allowed = actionCode != null && view.actionCodes().contains(actionCode);
        List<DecisionSource> sources = List.of();
        if (memberId.equals(actor.context().memberId())) {
            sources = List.of(new DecisionSource(null, null, null, "已按当前身份展开有效授权"));
        }
        return new Decision(allowed, allowed ? null : IamReasonCode.ACTION_DENIED,
                allowed ? "当前计算允许该操作" : "当前计算不允许该操作", sources, "不包含越界对象明细",
                Map.of(), view.version(), view.expiresAt());
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("domain", domain.name());
        if (domain == AuthorizationDomain.TENANT) {
            parameters.put("tenantId", IamIds.require(actor.context().tenantId()));
        }
        Long total = jdbc.queryForObject(domain == AuthorizationDomain.PLATFORM
                ? "SELECT COUNT(*) FROM iam_authorization_audit WHERE domain=:domain AND tenant_id IS NULL"
                : "SELECT COUNT(*) FROM iam_authorization_audit WHERE domain=:domain AND tenant_id=:tenantId",
                parameters, Long.class);
        parameters.put("limit", pageSize);
        parameters.put("offset", IamPages.offset(page, pageSize));
        List<ResourceDetail<AuditEntry>> items = jdbc.query(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT * FROM iam_authorization_audit WHERE domain=:domain AND tenant_id IS NULL
                 ORDER BY occurred_at DESC,id DESC LIMIT :limit OFFSET :offset
                """
                : """
                SELECT * FROM iam_authorization_audit WHERE domain=:domain AND tenant_id=:tenantId
                 ORDER BY occurred_at DESC,id DESC LIMIT :limit OFFSET :offset
                """, parameters, (row, index) -> IamDetails.of(entry(row), "0"));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    private String resolveMember(AuthorizationDomain domain, ActiveIdentity actor, DiagnoseInput input) {
        if (input.memberId() != null && !input.memberId().isBlank()) {
            long memberId = IamIds.require(input.memberId());
            Long count = domain == AuthorizationDomain.PLATFORM
                    ? jdbc.queryForObject("SELECT COUNT(*) FROM iam_platform_member WHERE id=:id AND status<>'REMOVED'",
                    Map.of("id", memberId), Long.class)
                    : jdbc.queryForObject("""
                    SELECT COUNT(*) FROM iam_tenant_member
                     WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                    """, Map.of("tenantId", IamIds.require(actor.context().tenantId()), "id", memberId), Long.class);
            if (count == null || count == 0) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return input.memberId();
        }
        long accountId = IamIds.require(input.accountId());
        List<String> members = domain == AuthorizationDomain.PLATFORM
                ? jdbc.queryForList("""
                SELECT id FROM iam_platform_member WHERE account_id=:accountId AND status<>'REMOVED'
                """, Map.of("accountId", accountId), String.class)
                : jdbc.queryForList("""
                SELECT id FROM iam_tenant_member
                 WHERE tenant_id=:tenantId AND account_id=:accountId AND status<>'REMOVED'
                """, Map.of("tenantId", IamIds.require(actor.context().tenantId()), "accountId", accountId),
                String.class);
        if (members.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return members.getFirst();
    }

    private String actionCode(long actionId) {
        List<String> codes = jdbc.queryForList("SELECT code FROM iam_action WHERE id=:id AND enabled=TRUE",
                Map.of("id", actionId), String.class);
        return codes.size() == 1 ? codes.getFirst() : null;
    }

    private AuditEntry entry(java.sql.ResultSet row) throws java.sql.SQLException {
        AuthorizationDomain domain = AuthorizationDomain.valueOf(row.getString("domain"));
        String tenantId = row.getObject("tenant_id") == null ? null : row.getString("tenant_id");
        Instant occurred = row.getTimestamp("occurred_at").toInstant();
        return new AuditEntry(row.getString("id"),
                new AuditActor(row.getString("actor_account_id"), row.getString("actor_member_id"), null),
                new AuthorizationContext(domain, tenantId, row.getString("actor_account_id"),
                        row.getString("actor_member_id")),
                new AuditTarget(row.getString("target_type"), row.getString("target_id")),
                AuditChangeType.valueOf(row.getString("change_type")),
                fields(row.getString("safe_before")), fields(row.getString("safe_after")),
                revisions(row.getString("revisions")),
                row.getObject("delegation_id") == null ? null : row.getString("delegation_id"),
                row.getObject("assignment_id") == null ? null : row.getString("assignment_id"),
                occurred, row.getString("trace_id"));
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
}
