package com.ingot.cloud.iam.delegation;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.cloud.iam.support.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DelegationInput;
import com.ingot.framework.commons.model.iam.DelegationRecord;
import com.ingot.framework.commons.model.iam.DelegationUpdateInput;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ReferenceImpactPreview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护不可拼接的委派限制，来源撤销会使派生授权立即无效。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class JdbcDelegationService {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String DELEGATION = "delegation";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、失效与委派表。
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public JdbcDelegationService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                                 DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域委派。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 委派页
     */
    public PageResponse<ResourceDetail<DelegationRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        IamPages.require(page, pageSize);
        Map<String, Object> parameters = domainParameters(domain, actor);
        Long total = jdbc.queryForObject(domain == AuthorizationDomain.PLATFORM
                ? "SELECT COUNT(*) FROM iam_delegation_grant WHERE domain=:domain AND tenant_id IS NULL"
                : "SELECT COUNT(*) FROM iam_delegation_grant WHERE domain=:domain AND tenant_id=:tenantId",
                parameters, Long.class);
        parameters.put("limit", pageSize);
        parameters.put("offset", IamPages.offset(page, pageSize));
        List<Long> ids = jdbc.queryForList(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT id FROM iam_delegation_grant WHERE domain=:domain AND tenant_id IS NULL
                 ORDER BY id LIMIT :limit OFFSET :offset
                """
                : """
                SELECT id FROM iam_delegation_grant WHERE domain=:domain AND tenant_id=:tenantId
                 ORDER BY id LIMIT :limit OFFSET :offset
                """, parameters, Long.class);
        List<ResourceDetail<DelegationRecord>> items = new ArrayList<>();
        for (Long id : ids) {
            items.add(load(domain, actor, id));
        }
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    /**
     * 读取委派详情。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @return 委派详情
     */
    public ResourceDetail<DelegationRecord> get(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        return load(domain, actor, IamIds.require(id));
    }

    /**
     * 创建委派。
     *
     * @param domain 接口管理域
     * @param input 委派限制
     * @return 新委派 ID
     */
    public CreatedResource create(AuthorizationDomain domain, DelegationInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.CREATE));
        validate(domain, actor, input);
        return transaction.execute(status -> {
            long id = access.nextId();
            insertGrant(domain, actor, id, input);
            replaceChildren(domain, actor, id, input);
            audits.write(actor.context(), access.nextId(), DELEGATION, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.RECIPIENT_SELECTION, "created"), Map.of(DELEGATION, "0"));
            changes.markAll();
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 调整委派；派生授权超出新上限时整批拒绝。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @param input 完整限制
     * @return 更新后详情
     */
    public ResourceDetail<DelegationRecord> replace(AuthorizationDomain domain, String id, DelegationUpdateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.UPDATE));
        validate(domain, actor, input.delegation());
        long delegationId = IamIds.require(id);
        return transaction.execute(status -> {
            ResourceDetail<DelegationRecord> current = lock(domain, actor, delegationId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            if (!derivedStillValid(domain, actor, delegationId, input.delegation())) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
            updateGrant(domain, actor, delegationId, input.delegation());
            replaceChildren(domain, actor, delegationId, input.delegation());
            audits.write(actor.context(), access.nextId(), DELEGATION, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.RECIPIENT_SELECTION, "before"),
                    Map.of(AuditField.RECIPIENT_SELECTION, "after"),
                    Map.of(DELEGATION, Long.toString(Long.parseLong(current.version()) + 1)));
            changes.markAll();
            return load(domain, actor, delegationId);
        });
    }

    /**
     * 撤销委派并使派生授权无效。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @return 撤销前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.DELETE));
        long delegationId = IamIds.require(id);
        return transaction.execute(status -> {
            ResourceDetail<DelegationRecord> current = lock(domain, actor, delegationId);
            jdbc.update("UPDATE iam_delegation_grant SET status=:revoked,version=version+1 WHERE id=:id",
                    Map.of("revoked", GrantStatus.REVOKED.name(), "id", delegationId));
            jdbc.update("""
                    UPDATE iam_role_assignment SET status=:revoked,version=version+1
                     WHERE delegation_grant_id=:id AND status=:active
                    """, Map.of("revoked", GrantStatus.REVOKED.name(), "id", delegationId,
                    "active", GrantStatus.ACTIVE.name()));
            audits.write(actor.context(), access.nextId(), DELEGATION, id, AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, GrantStatus.ACTIVE.name()),
                    Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()), Map.of(DELEGATION, current.version()));
            changes.markAll();
            return new CreatedResource(id, current.version());
        });
    }

    /**
     * 预览委派收缩影响，无写入。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @param input 待保存限制
     * @return 引用影响
     */
    public Preview<ReferenceImpactPreview> preview(AuthorizationDomain domain, String id, DelegationUpdateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.PREVIEW));
        ResourceDetail<DelegationRecord> current = load(domain, actor, IamIds.require(id));
        List<ValidationIssue> errors = new ArrayList<>();
        try {
            validate(domain, actor, input.delegation());
            IamIds.requireVersion(input.expectedVersion(), current.version());
        } catch (BizException exception) {
            errors.add(new ValidationIssue("delegation", IamReasonCode.INVALID_ARGUMENT, exception.getMessage()));
        }
        List<String> affected = derivedIds(IamIds.require(id));
        if (errors.isEmpty() && !derivedStillValid(domain, actor, IamIds.require(id), input.delegation())) {
            errors.add(new ValidationIssue("delegation", IamReasonCode.POLICY_CONFLICT, "存在超出新上限的派生授权"));
        }
        ReferenceImpactPreview result = new ReferenceImpactPreview(affected,
                new ImpactSummary(null, (long) affected.size(), 1L, false));
        return new Preview<>(current.version(), errors.isEmpty(), errors, List.of(), result.impactSummary(), result);
    }

    private void validate(AuthorizationDomain domain, ActiveIdentity actor, DelegationInput input) {
        IamSelections.requireCompatible(domain, input.recipientSelection());
        long adminId = IamIds.require(input.administratorMemberId());
        Long admin = domain == AuthorizationDomain.PLATFORM
                ? jdbc.queryForObject("SELECT COUNT(*) FROM iam_platform_member WHERE id=:id AND status<>'REMOVED'",
                Map.of("id", adminId), Long.class)
                : jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_tenant_member
                 WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                """, Map.of("tenantId", IamIds.require(actor.context().tenantId()), "id", adminId), Long.class);
        if (admin == null || admin == 0) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        for (RoleRevisionRef ref : input.allowedRoleRevisionRefs()) {
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_role_revision WHERE id=:id AND kind=:kind",
                    Map.of("id", IamIds.require(ref.id()), "kind", ref.kind().name()), Long.class);
            if (count == null || count == 0) {
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            }
        }
    }

    private ResourceDetail<DelegationRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        List<GrantRow> rows = jdbc.query(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT id,platform_administrator_id,tenant_administrator_id,valid_from,valid_until,
                       max_assignment_duration_seconds,max_assignment_duration_nanos,status,version
                  FROM iam_delegation_grant WHERE id=:id AND domain=:domain AND tenant_id IS NULL
                """
                : """
                SELECT id,platform_administrator_id,tenant_administrator_id,valid_from,valid_until,
                       max_assignment_duration_seconds,max_assignment_duration_nanos,status,version
                  FROM iam_delegation_grant WHERE id=:id AND domain=:domain AND tenant_id=:tenantId
                """, domainParameters(domain, actor, id),
                (row, index) -> new GrantRow(row.getLong("id"),
                        first(row.getObject("platform_administrator_id"), row.getObject("tenant_administrator_id")),
                        row.getTimestamp("valid_from") == null ? null : row.getTimestamp("valid_from").toInstant(),
                        row.getTimestamp("valid_until") == null ? null : row.getTimestamp("valid_until").toInstant(),
                        Duration.ofSeconds(row.getLong("max_assignment_duration_seconds"),
                                row.getInt("max_assignment_duration_nanos")),
                        GrantStatus.valueOf(row.getString("status")), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        GrantRow grant = rows.getFirst();
        DelegationInput input = new DelegationInput(IamIds.text(grant.administratorId()), revisions(id),
                recipients(domain, actor, id), ceilings(id), grant.validFrom(), grant.validUntil(), grant.maxDuration());
        return IamDetails.of(new DelegationRecord(IamIds.text(id), input, grant.status()), grant.version());
    }

    private ResourceDetail<DelegationRecord> lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        load(domain, actor, id);
        jdbc.queryForObject(domain == AuthorizationDomain.PLATFORM
                ? "SELECT id FROM iam_delegation_grant WHERE id=:id AND domain=:domain AND tenant_id IS NULL FOR UPDATE"
                : """
                SELECT id FROM iam_delegation_grant
                 WHERE id=:id AND domain=:domain AND tenant_id=:tenantId FOR UPDATE
                """, domainParameters(domain, actor, id), Long.class);
        return load(domain, actor, id);
    }

    private void insertGrant(AuthorizationDomain domain, ActiveIdentity actor, long id, DelegationInput input) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("domain", domain.name());
        parameters.put("tenantId", domain == AuthorizationDomain.TENANT
                ? IamIds.require(actor.context().tenantId()) : null);
        parameters.put("platformAdmin", domain == AuthorizationDomain.PLATFORM
                ? IamIds.require(input.administratorMemberId()) : null);
        parameters.put("tenantAdmin", domain == AuthorizationDomain.TENANT
                ? IamIds.require(input.administratorMemberId()) : null);
        parameters.put("validFrom", input.validFrom() == null ? null : Timestamp.from(input.validFrom()));
        parameters.put("validUntil", input.validUntil() == null ? null : Timestamp.from(input.validUntil()));
        parameters.put("seconds", input.maxAssignmentDuration().getSeconds());
        parameters.put("nanos", input.maxAssignmentDuration().getNano());
        parameters.put("status", GrantStatus.ACTIVE.name());
        jdbc.update("""
                INSERT INTO iam_delegation_grant(id,domain,tenant_id,platform_administrator_id,tenant_administrator_id,
                  valid_from,valid_until,max_assignment_duration_seconds,max_assignment_duration_nanos,status)
                VALUES (:id,:domain,:tenantId,:platformAdmin,:tenantAdmin,:validFrom,:validUntil,:seconds,:nanos,:status)
                """, parameters);
    }

    private void updateGrant(AuthorizationDomain domain, ActiveIdentity actor, long id, DelegationInput input) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("platformAdmin", domain == AuthorizationDomain.PLATFORM
                ? IamIds.require(input.administratorMemberId()) : null);
        parameters.put("tenantAdmin", domain == AuthorizationDomain.TENANT
                ? IamIds.require(input.administratorMemberId()) : null);
        parameters.put("validFrom", input.validFrom() == null ? null : Timestamp.from(input.validFrom()));
        parameters.put("validUntil", input.validUntil() == null ? null : Timestamp.from(input.validUntil()));
        parameters.put("seconds", input.maxAssignmentDuration().getSeconds());
        parameters.put("nanos", input.maxAssignmentDuration().getNano());
        jdbc.update("""
                UPDATE iam_delegation_grant
                   SET platform_administrator_id=:platformAdmin,tenant_administrator_id=:tenantAdmin,
                       valid_from=:validFrom,valid_until=:validUntil,
                       max_assignment_duration_seconds=:seconds,max_assignment_duration_nanos=:nanos,
                       version=version+1
                 WHERE id=:id
                """, parameters);
    }

    private void replaceChildren(AuthorizationDomain domain, ActiveIdentity actor, long id, DelegationInput input) {
        jdbc.update("DELETE FROM iam_delegation_role_revision WHERE delegation_id=:id", Map.of("id", id));
        jdbc.update("DELETE FROM iam_delegation_recipient_member WHERE delegation_id=:id", Map.of("id", id));
        jdbc.update("DELETE FROM iam_delegation_recipient_department WHERE delegation_id=:id", Map.of("id", id));
        jdbc.update("DELETE FROM iam_delegation_action_ceiling WHERE delegation_id=:id", Map.of("id", id));
        for (RoleRevisionRef ref : input.allowedRoleRevisionRefs()) {
            jdbc.update("""
                    INSERT INTO iam_delegation_role_revision(delegation_id,revision_id,revision_kind)
                    VALUES (:id,:revisionId,:kind)
                    """, Map.of("id", id, "revisionId", IamIds.require(ref.id()), "kind", ref.kind().name()));
        }
        for (String memberId : input.recipientSelection().members()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("domain", domain.name());
            parameters.put("tenantId", domain == AuthorizationDomain.TENANT
                    ? IamIds.require(actor.context().tenantId()) : null);
            parameters.put("platformMemberId", domain == AuthorizationDomain.PLATFORM ? IamIds.require(memberId) : null);
            parameters.put("tenantMemberId", domain == AuthorizationDomain.TENANT ? IamIds.require(memberId) : null);
            jdbc.update("""
                    INSERT INTO iam_delegation_recipient_member(delegation_id,domain,tenant_id,platform_member_id,
                      tenant_member_id)
                    VALUES (:id,:domain,:tenantId,:platformMemberId,:tenantMemberId)
                    """, parameters);
        }
        for (DepartmentSelection department : input.recipientSelection().departments()) {
            jdbc.update("""
                    INSERT INTO iam_delegation_recipient_department(delegation_id,tenant_id,department_id,include_descendants)
                    VALUES (:id,:tenantId,:departmentId,:descendants)
                    """, Map.of("id", id, "tenantId", IamIds.require(actor.context().tenantId()),
                    "departmentId", IamIds.require(department.id()), "descendants", department.includeDescendants()));
        }
        for (ActionScopeCeiling ceiling : input.actionScopeCeilings()) {
            jdbc.update("""
                    INSERT INTO iam_delegation_action_ceiling(delegation_id,action_id,scopes,scope_bindings)
                    VALUES (:id,:actionId,:scopes,:bindings)
                    """, Map.of("id", id, "actionId", IamIds.require(ceiling.actionId()),
                    "scopes", IamJson.array(ceiling.scopes()), "bindings", IamJson.object(ceiling.scopeBindings())));
        }
    }

    private List<RoleRevisionRef> revisions(long id) {
        return jdbc.query("""
                SELECT revision_id,revision_kind FROM iam_delegation_role_revision
                 WHERE delegation_id=:id ORDER BY revision_id
                """, Map.of("id", id), (row, index) -> new RoleRevisionRef(
                RoleKind.valueOf(row.getString("revision_kind")), row.getString("revision_id")));
    }

    private Selection recipients(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        List<String> members = jdbc.queryForList(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT platform_member_id FROM iam_delegation_recipient_member
                 WHERE delegation_id=:id ORDER BY platform_member_id
                """
                : """
                SELECT tenant_member_id FROM iam_delegation_recipient_member
                 WHERE delegation_id=:id ORDER BY tenant_member_id
                """, Map.of("id", id), String.class);
        List<DepartmentSelection> departments = domain == AuthorizationDomain.PLATFORM ? List.of()
                : jdbc.query("""
                SELECT department_id,include_descendants FROM iam_delegation_recipient_department
                 WHERE delegation_id=:id ORDER BY department_id
                """, Map.of("id", id), (row, index) -> new DepartmentSelection(row.getString("department_id"),
                row.getBoolean("include_descendants")));
        return new Selection(members, departments);
    }

    private List<ActionScopeCeiling> ceilings(long id) {
        return jdbc.query("""
                SELECT action_id,scopes,scope_bindings FROM iam_delegation_action_ceiling
                 WHERE delegation_id=:id ORDER BY action_id
                """, Map.of("id", id), (row, index) -> new ActionScopeCeiling(row.getString("action_id"),
                IamJson.read(row.getString("scopes"), SCOPES),
                IamJson.read(row.getString("scope_bindings"), BINDINGS)));
    }

    private boolean derivedStillValid(AuthorizationDomain domain, ActiveIdentity actor, long delegationId,
                                      DelegationInput next) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id,revision_id,valid_from,valid_until FROM iam_role_assignment
                 WHERE delegation_grant_id=:id AND status=:active
                """, Map.of("id", delegationId, "active", GrantStatus.ACTIVE.name()));
        for (Map<String, Object> row : rows) {
            long revisionId = ((Number) row.get("revision_id")).longValue();
            boolean allowed = next.allowedRoleRevisionRefs().stream()
                    .anyMatch(ref -> IamIds.require(ref.id()) == revisionId);
            if (!allowed) {
                return false;
            }
            Instant from = ((Timestamp) row.get("valid_from")).toInstant();
            Timestamp untilValue = (Timestamp) row.get("valid_until");
            if (untilValue == null
                    || Duration.between(from, untilValue.toInstant()).compareTo(next.maxAssignmentDuration()) > 0) {
                return false;
            }
        }
        return true;
    }

    private List<String> derivedIds(long delegationId) {
        return jdbc.queryForList("""
                SELECT id FROM iam_role_assignment WHERE delegation_grant_id=:id AND status=:active
                """, Map.of("id", delegationId, "active", GrantStatus.ACTIVE.name()), String.class);
    }

    private Map<String, Object> domainParameters(AuthorizationDomain domain, ActiveIdentity actor) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("domain", domain.name());
        if (domain == AuthorizationDomain.TENANT) {
            parameters.put("tenantId", IamIds.require(actor.context().tenantId()));
        }
        return parameters;
    }

    private Map<String, Object> domainParameters(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        Map<String, Object> parameters = domainParameters(domain, actor);
        parameters.put("id", id);
        return parameters;
    }

    private static long first(Object left, Object right) {
        Object value = left != null ? left : right;
        return ((Number) value).longValue();
    }

    private static IamAction action(AuthorizationDomain domain, AccessKind kind) {
        return switch (kind) {
            case READ -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_READ
                    : IamAction.TENANT_DELEGATION_READ;
            case CREATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_CREATE
                    : IamAction.TENANT_DELEGATION_CREATE;
            case UPDATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_UPDATE
                    : IamAction.TENANT_DELEGATION_UPDATE;
            case DELETE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_DELETE
                    : IamAction.TENANT_DELEGATION_DELETE;
            case PREVIEW -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_PREVIEW
                    : IamAction.TENANT_DELEGATION_PREVIEW;
        };
    }

    private enum AccessKind {
        READ, CREATE, UPDATE, DELETE, PREVIEW
    }

    private record GrantRow(long id, long administratorId, Instant validFrom, Instant validUntil, Duration maxDuration,
                            GrantStatus status, String version) {
    }
}
