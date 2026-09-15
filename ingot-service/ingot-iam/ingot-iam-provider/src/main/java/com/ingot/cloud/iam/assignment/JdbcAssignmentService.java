package com.ingot.cloud.iam.assignment;

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
import com.ingot.cloud.iam.role.JdbcRoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AssignmentBatchInput;
import com.ingot.framework.commons.model.iam.AssignmentInput;
import com.ingot.framework.commons.model.iam.AssignmentPreviewItem;
import com.ingot.framework.commons.model.iam.AssignmentPreviewResult;
import com.ingot.framework.commons.model.iam.AssignmentRecord;
import com.ingot.framework.commons.model.iam.AssignmentSource;
import com.ingot.framework.commons.model.iam.AssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.SubjectRef;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护原子角色分配，提交时重验主体、固定版本、范围和委派来源。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class JdbcAssignmentService {
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String ASSIGNMENT = "assignment";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final JdbcRoleService roles;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、角色合成与分配表。
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param roles 角色版本合成
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public JdbcAssignmentService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                                 JdbcRoleService roles, DataSource dataSource,
                                 PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.roles = roles;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域授权。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 授权页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        IamPages.require(page, pageSize);
        Map<String, Object> parameters = domainParameters(domain, actor);
        Long total = jdbc.queryForObject(domain == AuthorizationDomain.PLATFORM
                ? "SELECT COUNT(*) FROM iam_role_assignment WHERE domain=:domain AND tenant_id IS NULL"
                : "SELECT COUNT(*) FROM iam_role_assignment WHERE domain=:domain AND tenant_id=:tenantId",
                parameters, Long.class);
        parameters.put("limit", pageSize);
        parameters.put("offset", IamPages.offset(page, pageSize));
        List<ResourceDetail<AssignmentRecord>> items = jdbc.query(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT * FROM iam_role_assignment WHERE domain=:domain AND tenant_id IS NULL
                 ORDER BY id LIMIT :limit OFFSET :offset
                """
                : """
                SELECT * FROM iam_role_assignment WHERE domain=:domain AND tenant_id=:tenantId
                 ORDER BY id LIMIT :limit OFFSET :offset
                """, parameters, (row, index) -> IamDetails.of(record(row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    /**
     * 原子批量创建授权，任一条失败整批回滚。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 首条授权 ID
     */
    public CreatedResource create(AuthorizationDomain domain, AssignmentBatchInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.CREATE));
        return transaction.execute(status -> {
            CreatedResource first = null;
            int index = 0;
            for (AssignmentInput item : input.items()) {
                List<ValidationIssue> errors = validate(domain, actor, item, null);
                if (!errors.isEmpty()) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
                CreatedResource created = insert(domain, actor, item);
                if (index == 0) {
                    first = created;
                }
                index++;
            }
            changes.markAll();
            return first;
        });
    }

    /**
     * 预览批量分配，无写入。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 逐条效果
     */
    public Preview<AssignmentPreviewResult> preview(AuthorizationDomain domain, AssignmentBatchInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.CREATE));
        List<AssignmentPreviewItem> items = new ArrayList<>();
        List<ValidationIssue> errors = new ArrayList<>();
        int index = 0;
        for (AssignmentInput item : input.items()) {
            List<ValidationIssue> itemErrors = validate(domain, actor, item, null);
            List<ActionGrant> grants = List.of();
            if (itemErrors.isEmpty()) {
                grants = roles.synthesizedGrants(IamIds.require(item.roleRevisionRef().id()));
            } else {
                errors.addAll(itemErrors);
            }
            items.add(new AssignmentPreviewItem(item.subject(), itemErrors.isEmpty(), itemErrors, grants));
            index++;
        }
        AssignmentPreviewResult result = new AssignmentPreviewResult(items);
        return new Preview<>("0", errors.isEmpty(), errors, List.of(),
                new ImpactSummary(null, (long) input.items().size(), null, false), result);
    }

    /**
     * 调整既有授权，禁止改写主体或伪造委派来源。
     *
     * @param domain 接口管理域
     * @param id 授权 ID
     * @param input 待保存定义
     * @return 更新后详情
     */
    public ResourceDetail<AssignmentRecord> replace(AuthorizationDomain domain, String id, AssignmentUpdateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.UPDATE));
        long assignmentId = IamIds.require(id);
        return transaction.execute(status -> {
            AssignmentRow current = lock(domain, actor, assignmentId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            AssignmentInput next = input.assignment();
            if (!sameSubject(current, next.subject()) || !sameDelegation(current.delegationId(), next.delegationGrantId())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            List<ValidationIssue> errors = validate(domain, actor, next, assignmentId);
            if (!errors.isEmpty()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            Instant from = next.validFrom() == null ? Instant.now() : next.validFrom();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", assignmentId);
            parameters.put("revisionId", IamIds.require(next.roleRevisionRef().id()));
            parameters.put("revisionKind", next.roleRevisionRef().kind().name());
            parameters.put("bindings", IamJson.object(next.scopeBindings()));
            parameters.put("validFrom", Timestamp.from(from));
            parameters.put("validUntil", next.validUntil() == null ? null : Timestamp.from(next.validUntil()));
            jdbc.update("""
                    UPDATE iam_role_assignment
                       SET revision_id=:revisionId,revision_kind=:revisionKind,scope_bindings=:bindings,
                           valid_from=:validFrom,valid_until=:validUntil,version=version+1
                     WHERE id=:id
                    """, parameters);
            audits.write(actor.context(), access.nextId(), ASSIGNMENT, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, IamIds.text(current.revisionId())),
                    Map.of(AuditField.ROLE_REVISION, next.roleRevisionRef().id()),
                    Map.of(ASSIGNMENT, Long.toString(Long.parseLong(current.version()) + 1)));
            changes.markAll();
            return get(domain, actor, assignmentId);
        });
    }

    /**
     * 撤销授权并保留审计行。
     *
     * @param domain 接口管理域
     * @param id 授权 ID
     * @return 撤销前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.DELETE));
        long assignmentId = IamIds.require(id);
        return transaction.execute(status -> {
            AssignmentRow current = lock(domain, actor, assignmentId);
            jdbc.update("UPDATE iam_role_assignment SET status=:revoked,version=version+1 WHERE id=:id",
                    Map.of("revoked", GrantStatus.REVOKED.name(), "id", assignmentId));
            audits.write(actor.context(), access.nextId(), ASSIGNMENT, id, AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, current.status().name()),
                    Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()),
                    Map.of(ASSIGNMENT, Long.toString(Long.parseLong(current.version()) + 1)));
            changes.markAll();
            return new CreatedResource(id, current.version());
        });
    }

    private ResourceDetail<AssignmentRecord> get(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        List<ResourceDetail<AssignmentRecord>> rows = jdbc.query(
                domain == AuthorizationDomain.PLATFORM
                        ? "SELECT * FROM iam_role_assignment WHERE id=:id AND domain=:domain AND tenant_id IS NULL"
                        : "SELECT * FROM iam_role_assignment WHERE id=:id AND domain=:domain AND tenant_id=:tenantId",
                domainParameters(domain, actor, id),
                (row, index) -> IamDetails.of(record(row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private CreatedResource insert(AuthorizationDomain domain, ActiveIdentity actor, AssignmentInput item) {
        long id = access.nextId();
        Instant from = item.validFrom() == null ? Instant.now() : item.validFrom();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("domain", domain.name());
        parameters.put("tenantId", domain == AuthorizationDomain.TENANT
                ? IamIds.require(actor.context().tenantId()) : null);
        parameters.put("subjectType", item.subject().type().name());
        parameters.put("platformMemberId", null);
        parameters.put("platformGroupId", null);
        parameters.put("tenantMemberId", null);
        parameters.put("tenantGroupId", null);
        long subjectId = IamIds.require(item.subject().id());
        if (domain == AuthorizationDomain.PLATFORM && item.subject().type() == SubjectType.MEMBER) {
            parameters.put("platformMemberId", subjectId);
        } else if (domain == AuthorizationDomain.PLATFORM) {
            parameters.put("platformGroupId", subjectId);
        } else if (item.subject().type() == SubjectType.MEMBER) {
            parameters.put("tenantMemberId", subjectId);
        } else {
            parameters.put("tenantGroupId", subjectId);
        }
        parameters.put("revisionId", IamIds.require(item.roleRevisionRef().id()));
        parameters.put("revisionKind", item.roleRevisionRef().kind().name());
        parameters.put("bindings", IamJson.object(item.scopeBindings()));
        parameters.put("delegationId", item.delegationGrantId() == null || item.delegationGrantId().isBlank()
                ? null : IamIds.require(item.delegationGrantId()));
        parameters.put("validFrom", Timestamp.from(from));
        parameters.put("validUntil", item.validUntil() == null ? null : Timestamp.from(item.validUntil()));
        parameters.put("status", GrantStatus.ACTIVE.name());
        parameters.put("source", AssignmentSource.MANUAL.name());
        jdbc.update("""
                INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,platform_member_id,platform_group_id,
                  tenant_member_id,tenant_group_id,revision_id,revision_kind,scope_bindings,delegation_grant_id,
                  valid_from,valid_until,status,source)
                VALUES (:id,:domain,:tenantId,:subjectType,:platformMemberId,:platformGroupId,:tenantMemberId,
                        :tenantGroupId,:revisionId,:revisionKind,:bindings,:delegationId,:validFrom,:validUntil,
                        :status,:source)
                """, parameters);
        audits.write(actor.context(), access.nextId(), ASSIGNMENT, IamIds.text(id), AuditChangeType.CREATE,
                Map.of(), Map.of(AuditField.ROLE_REVISION, item.roleRevisionRef().id()), Map.of(ASSIGNMENT, "0"));
        return new CreatedResource(IamIds.text(id), "0");
    }

    private List<ValidationIssue> validate(AuthorizationDomain domain, ActiveIdentity actor, AssignmentInput item,
                                           Long currentId) {
        List<ValidationIssue> errors = new ArrayList<>();
        if (item == null || item.subject() == null || item.roleRevisionRef() == null) {
            errors.add(new ValidationIssue("assignment", IamReasonCode.INVALID_ARGUMENT, "分配定义不完整"));
            return errors;
        }
        if (domain == AuthorizationDomain.PLATFORM && hasDepartmentBinding(item.scopeBindings())) {
            errors.add(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "平台不得使用部门参数"));
        }
        if (!subjectExists(domain, actor, item.subject())) {
            errors.add(new ValidationIssue("subject", IamReasonCode.OBJECT_NOT_FOUND, "接收主体不属于当前域"));
        }
        RevisionRef revision = loadRevision(item.roleRevisionRef());
        if (revision == null || revision.kind() != item.roleRevisionRef().kind()) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.ROLE_REVISION_UNAVAILABLE, "角色版本不可用"));
        } else if (!revision.enabled()) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.ACTION_DENIED, "角色已停用"));
        } else if (!revisionAssignable(domain, actor, revision)) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.INVALID_ARGUMENT, "角色版本不属于当前域"));
        }
        Instant from = item.validFrom() == null ? Instant.now() : item.validFrom();
        if (item.validUntil() != null && !from.isBefore(item.validUntil())) {
            errors.add(new ValidationIssue("validUntil", IamReasonCode.INVALID_ARGUMENT, "起止时间必须形成左闭右开区间"));
        }
        if (item.delegationGrantId() != null && !item.delegationGrantId().isBlank()) {
            errors.addAll(validateDelegation(domain, actor, item, from));
        }
        return errors;
    }

    private List<ValidationIssue> validateDelegation(AuthorizationDomain domain, ActiveIdentity actor,
                                                     AssignmentInput item, Instant from) {
        List<ValidationIssue> errors = new ArrayList<>();
        long delegationId = IamIds.require(item.delegationGrantId());
        List<DelegationRow> rows = jdbc.query(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT id,status,valid_from,valid_until,max_assignment_duration_seconds,max_assignment_duration_nanos
                  FROM iam_delegation_grant WHERE id=:id AND domain=:domain AND tenant_id IS NULL
                """
                : """
                SELECT id,status,valid_from,valid_until,max_assignment_duration_seconds,max_assignment_duration_nanos
                  FROM iam_delegation_grant WHERE id=:id AND domain=:domain AND tenant_id=:tenantId
                """, domainParameters(domain, actor, delegationId),
                (row, index) -> new DelegationRow(row.getLong("id"), GrantStatus.valueOf(row.getString("status")),
                        row.getTimestamp("valid_from") == null ? null : row.getTimestamp("valid_from").toInstant(),
                        row.getTimestamp("valid_until") == null ? null : row.getTimestamp("valid_until").toInstant(),
                        Duration.ofSeconds(row.getLong("max_assignment_duration_seconds"),
                                row.getInt("max_assignment_duration_nanos"))));
        if (rows.size() != 1 || rows.getFirst().status() != GrantStatus.ACTIVE) {
            errors.add(new ValidationIssue("delegationGrantId", IamReasonCode.OBJECT_NOT_FOUND, "委派不存在或已撤销"));
            return errors;
        }
        DelegationRow delegation = rows.getFirst();
        Instant now = Instant.now();
        if (delegation.validFrom() != null && now.isBefore(delegation.validFrom())
                || delegation.validUntil() != null && !now.isBefore(delegation.validUntil())) {
            errors.add(new ValidationIssue("delegationGrantId", IamReasonCode.ACTION_DENIED, "委派不在有效期内"));
        }
        Instant until = item.validUntil();
        if (until == null) {
            errors.add(new ValidationIssue("validUntil", IamReasonCode.INVALID_ARGUMENT, "来源委派的分配必须有结束时间"));
        } else if (Duration.between(from, until).compareTo(delegation.maxDuration()) > 0) {
            errors.add(new ValidationIssue("validUntil", IamReasonCode.INVALID_ARGUMENT, "分配期限超过委派上限"));
        }
        Long allowed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_delegation_role_revision WHERE delegation_id=:id AND revision_id=:revisionId",
                Map.of("id", delegationId, "revisionId", IamIds.require(item.roleRevisionRef().id())), Long.class);
        if (allowed == null || allowed == 0) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.ACTION_DENIED, "委派不允许该角色版本"));
        }
        if (!recipientAllowed(domain, actor, delegationId, item.subject())) {
            errors.add(new ValidationIssue("subject", IamReasonCode.ACTION_DENIED, "接收者不在委派人群内"));
        }
        return errors;
    }

    private boolean recipientAllowed(AuthorizationDomain domain, ActiveIdentity actor, long delegationId,
                                     SubjectRef subject) {
        if (subject.type() != SubjectType.MEMBER) {
            List<String> members = groupMembers(domain, actor, IamIds.require(subject.id()));
            return !members.isEmpty() && members.stream()
                    .allMatch(memberId -> memberRecipient(domain, actor, delegationId, IamIds.require(memberId)));
        }
        return memberRecipient(domain, actor, delegationId, IamIds.require(subject.id()));
    }

    private boolean memberRecipient(AuthorizationDomain domain, ActiveIdentity actor, long delegationId, long memberId) {
        Map<String, Object> parameters = domainParameters(domain, actor, delegationId);
        parameters.put("memberId", memberId);
        Long direct = jdbc.queryForObject(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT COUNT(*) FROM iam_delegation_recipient_member
                 WHERE delegation_id=:id AND platform_member_id=:memberId
                """
                : """
                SELECT COUNT(*) FROM iam_delegation_recipient_member
                 WHERE delegation_id=:id AND tenant_id=:tenantId AND tenant_member_id=:memberId
                """, parameters, Long.class);
        if (direct != null && direct > 0) {
            return true;
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            return false;
        }
        Long department = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_delegation_recipient_department d
                  JOIN iam_member_department md ON md.tenant_id=d.tenant_id AND md.department_id=d.department_id
                 WHERE d.delegation_id=:id AND d.tenant_id=:tenantId AND md.member_id=:memberId
                """, parameters, Long.class);
        return department != null && department > 0;
    }

    private List<String> groupMembers(AuthorizationDomain domain, ActiveIdentity actor, long groupId) {
        return domain == AuthorizationDomain.PLATFORM
                ? jdbc.queryForList("SELECT member_id FROM iam_platform_group_member WHERE group_id=:id",
                Map.of("id", groupId), String.class)
                : jdbc.queryForList("""
                SELECT member_id FROM iam_tenant_group_member WHERE tenant_id=:tenantId AND group_id=:id
                """, Map.of("tenantId", IamIds.require(actor.context().tenantId()), "id", groupId), String.class);
    }

    private boolean subjectExists(AuthorizationDomain domain, ActiveIdentity actor, SubjectRef subject) {
        long id = IamIds.require(subject.id());
        Long count;
        if (domain == AuthorizationDomain.PLATFORM && subject.type() == SubjectType.MEMBER) {
            count = jdbc.queryForObject("SELECT COUNT(*) FROM iam_platform_member WHERE id=:id AND status<>'REMOVED'",
                    Map.of("id", id), Long.class);
        } else if (domain == AuthorizationDomain.PLATFORM) {
            count = jdbc.queryForObject("SELECT COUNT(*) FROM iam_platform_group WHERE id=:id",
                    Map.of("id", id), Long.class);
        } else if (subject.type() == SubjectType.MEMBER) {
            count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM iam_tenant_member
                     WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                    """, Map.of("tenantId", IamIds.require(actor.context().tenantId()), "id", id), Long.class);
        } else {
            count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_tenant_group WHERE tenant_id=:tenantId AND id=:id",
                    Map.of("tenantId", IamIds.require(actor.context().tenantId()), "id", id), Long.class);
        }
        return count != null && count > 0;
    }

    private RevisionRef loadRevision(RoleRevisionRef ref) {
        List<RevisionRef> rows = jdbc.query("""
                SELECT r.id,r.kind,d.enabled,d.domain,d.tenant_id FROM iam_role_revision r
                  JOIN iam_role_definition d ON d.id=r.role_id
                 WHERE r.id=:id
                """, Map.of("id", IamIds.require(ref.id())),
                (row, index) -> new RevisionRef(row.getLong("id"), RoleKind.valueOf(row.getString("kind")),
                        row.getBoolean("enabled"), AuthorizationDomain.valueOf(row.getString("domain")),
                        row.getObject("tenant_id") == null ? null : row.getLong("tenant_id")));
        return rows.size() == 1 ? rows.getFirst() : null;
    }

    private boolean revisionAssignable(AuthorizationDomain domain, ActiveIdentity actor, RevisionRef revision) {
        if (revision.kind() == RoleKind.SHARED) {
            return true;
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            return revision.domain() == AuthorizationDomain.PLATFORM && revision.tenantId() == null;
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        return (revision.kind() == RoleKind.SYSTEM && revision.domain() == AuthorizationDomain.TENANT)
                || (revision.kind() == RoleKind.TENANT_CUSTOM && tenantId == revision.tenantId());
    }

    private static boolean hasDepartmentBinding(Map<String, ScopeBinding> bindings) {
        if (bindings == null) {
            return false;
        }
        return bindings.values().stream().anyMatch(binding -> binding.kind() == ScopeBindingKind.DEPARTMENTS);
    }

    private AssignmentRow lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        List<AssignmentRow> rows = jdbc.query(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT id,revision_id,status,version,subject_type,platform_member_id,platform_group_id,
                       tenant_member_id,tenant_group_id,delegation_grant_id
                  FROM iam_role_assignment WHERE id=:id AND domain=:domain AND tenant_id IS NULL FOR UPDATE
                """
                : """
                SELECT id,revision_id,status,version,subject_type,platform_member_id,platform_group_id,
                       tenant_member_id,tenant_group_id,delegation_grant_id
                  FROM iam_role_assignment WHERE id=:id AND domain=:domain AND tenant_id=:tenantId FOR UPDATE
                """, domainParameters(domain, actor, id),
                (row, index) -> new AssignmentRow(row.getLong("id"), row.getLong("revision_id"),
                        GrantStatus.valueOf(row.getString("status")), row.getString("version"),
                        SubjectType.valueOf(row.getString("subject_type")),
                        row.getObject("platform_member_id") == null ? null : row.getLong("platform_member_id"),
                        row.getObject("platform_group_id") == null ? null : row.getLong("platform_group_id"),
                        row.getObject("tenant_member_id") == null ? null : row.getLong("tenant_member_id"),
                        row.getObject("tenant_group_id") == null ? null : row.getLong("tenant_group_id"),
                        row.getObject("delegation_grant_id") == null ? null : row.getLong("delegation_grant_id")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private AssignmentRecord record(java.sql.ResultSet row) throws java.sql.SQLException {
        SubjectType type = SubjectType.valueOf(row.getString("subject_type"));
        String subjectId = type == SubjectType.MEMBER
                ? firstNonNull(row.getObject("platform_member_id"), row.getObject("tenant_member_id"))
                : firstNonNull(row.getObject("platform_group_id"), row.getObject("tenant_group_id"));
        Instant from = row.getTimestamp("valid_from").toInstant();
        Instant until = row.getTimestamp("valid_until") == null ? null : row.getTimestamp("valid_until").toInstant();
        String delegation = row.getObject("delegation_grant_id") == null ? null : row.getString("delegation_grant_id");
        AssignmentInput assignment = new AssignmentInput(new SubjectRef(type, subjectId),
                new RoleRevisionRef(RoleKind.valueOf(row.getString("revision_kind")), row.getString("revision_id")),
                IamJson.read(row.getString("scope_bindings"), BINDINGS), from, until, delegation);
        return new AssignmentRecord(row.getString("id"), assignment, GrantStatus.valueOf(row.getString("status")),
                AssignmentSource.valueOf(row.getString("source")));
    }

    private static boolean sameSubject(AssignmentRow current, SubjectRef subject) {
        long id = IamIds.require(subject.id());
        if (current.subjectType() != subject.type()) {
            return false;
        }
        Long actual = subject.type() == SubjectType.MEMBER
                ? first(current.platformMemberId(), current.tenantMemberId())
                : first(current.platformGroupId(), current.tenantGroupId());
        return actual != null && actual == id;
    }

    private static boolean sameDelegation(Long current, String next) {
        if (current == null) {
            return next == null || next.isBlank();
        }
        return next != null && IamIds.require(next) == current;
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

    private static String firstNonNull(Object left, Object right) {
        Object value = left != null ? left : right;
        return value == null ? null : value.toString();
    }

    private static Long first(Long left, Long right) {
        return left != null ? left : right;
    }

    private static IamAction action(AuthorizationDomain domain, AccessKind kind) {
        return switch (kind) {
            case READ -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_READ
                    : IamAction.TENANT_ASSIGNMENT_READ;
            case CREATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_CREATE
                    : IamAction.TENANT_ASSIGNMENT_CREATE;
            case UPDATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_UPDATE
                    : IamAction.TENANT_ASSIGNMENT_UPDATE;
            case DELETE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_DELETE
                    : IamAction.TENANT_ASSIGNMENT_DELETE;
        };
    }

    private enum AccessKind {
        READ, CREATE, UPDATE, DELETE
    }

    private record AssignmentRow(long id, long revisionId, GrantStatus status, String version, SubjectType subjectType,
                                 Long platformMemberId, Long platformGroupId, Long tenantMemberId, Long tenantGroupId,
                                 Long delegationId) {
    }

    private record RevisionRef(long id, RoleKind kind, boolean enabled, AuthorizationDomain domain, Long tenantId) {
    }

    private record DelegationRow(long id, GrantStatus status, Instant validFrom, Instant validUntil,
                                 Duration maxDuration) {
    }
}
