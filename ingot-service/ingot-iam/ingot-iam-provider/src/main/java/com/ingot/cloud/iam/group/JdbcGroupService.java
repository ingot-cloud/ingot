package com.ingot.cloud.iam.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.cloud.iam.support.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.GroupDraft;
import com.ingot.framework.commons.model.iam.GroupRecord;
import com.ingot.framework.commons.model.iam.GroupUpdateInput;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ReferenceImpactPreview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护当前域静态用户组，修改后重验委派派生授权且平台组不得引用部门。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class JdbcGroupService {
    private static final String GROUP = "group";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、失效与组表。
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public JdbcGroupService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                            DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域用户组。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 组页
     */
    public PageResponse<ResourceDetail<GroupRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        IamPages.require(page, pageSize);
        Map<String, Object> parameters = domainParameters(domain, actor);
        Long total = jdbc.queryForObject(domain == AuthorizationDomain.PLATFORM
                ? "SELECT COUNT(*) FROM iam_platform_group"
                : "SELECT COUNT(*) FROM iam_tenant_group WHERE tenant_id=:tenantId", parameters, Long.class);
        parameters.put("limit", pageSize);
        parameters.put("offset", IamPages.offset(page, pageSize));
        List<ResourceDetail<GroupRecord>> items = domain == AuthorizationDomain.PLATFORM
                ? jdbc.query("""
                SELECT id,name,description,version FROM iam_platform_group
                 ORDER BY id LIMIT :limit OFFSET :offset
                """, parameters, (row, index) -> detail(domain, actor, row.getLong("id"), row.getString("name"),
                        row.getString("description"), row.getString("version")))
                : jdbc.query("""
                SELECT id,name,description,version FROM iam_tenant_group WHERE tenant_id=:tenantId
                 ORDER BY id LIMIT :limit OFFSET :offset
                """, parameters, (row, index) -> detail(domain, actor, row.getLong("id"), row.getString("name"),
                        row.getString("description"), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    /**
     * 读取用户组详情。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @return 组详情
     */
    public ResourceDetail<GroupRecord> get(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        return load(domain, actor, IamIds.require(id));
    }

    /**
     * 创建静态用户组。
     *
     * @param domain 接口管理域
     * @param input 组草稿
     * @return 新组 ID
     */
    public CreatedResource create(AuthorizationDomain domain, GroupDraft input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.CREATE));
        IamSelections.requireCompatible(domain, input.selection());
        return transaction.execute(status -> {
            long id = access.nextId();
            insertGroup(domain, actor, id, input);
            replaceSelection(domain, actor, id, input.selection());
            audits.write(actor.context(), access.nextId(), GROUP, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(GROUP, "0"));
            changes.markAll();
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 整体替换组内容并重验委派派生授权。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @param input 完整内容
     * @return 替换后详情
     */
    public ResourceDetail<GroupRecord> replace(AuthorizationDomain domain, String id, GroupUpdateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.UPDATE));
        IamSelections.requireCompatible(domain, input.group().selection());
        long groupId = IamIds.require(id);
        return transaction.execute(status -> {
            ResourceDetail<GroupRecord> current = lock(domain, actor, groupId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            List<String> invalid = invalidDelegatedAssignments(domain, actor, groupId, input.group().selection());
            if (!invalid.isEmpty()) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
            updateGroup(domain, actor, groupId, input.group());
            replaceSelection(domain, actor, groupId, input.group().selection());
            String version = Long.toString(Long.parseLong(current.version()) + 1);
            audits.write(actor.context(), access.nextId(), GROUP, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.record().name()),
                    Map.of(AuditField.NAME, input.group().name()), Map.of(GROUP, version));
            changes.markAll();
            return load(domain, actor, groupId);
        });
    }

    /**
     * 删除未被授权引用的用户组。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @return 删除前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.DELETE));
        long groupId = IamIds.require(id);
        return transaction.execute(status -> {
            ResourceDetail<GroupRecord> current = lock(domain, actor, groupId);
            if (referenced(domain, actor, groupId)) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            clearSelection(domain, actor, groupId);
            if (domain == AuthorizationDomain.PLATFORM) {
                jdbc.update("DELETE FROM iam_platform_group WHERE id=:id", Map.of("id", groupId));
            } else {
                jdbc.update("DELETE FROM iam_tenant_group WHERE tenant_id=:tenantId AND id=:id",
                        domainParameters(domain, actor, groupId));
            }
            audits.write(actor.context(), access.nextId(), GROUP, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.record().name()), Map.of(), Map.of(GROUP, current.version()));
            changes.markAll();
            return new CreatedResource(id, current.version());
        });
    }

    /**
     * 预览组替换对委派派生授权的影响，无写入。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @param input 待保存内容
     * @return 引用影响
     */
    public Preview<ReferenceImpactPreview> preview(AuthorizationDomain domain, String id, GroupUpdateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.PREVIEW));
        IamSelections.requireCompatible(domain, input.group().selection());
        ResourceDetail<GroupRecord> current = load(domain, actor, IamIds.require(id));
        List<ValidationIssue> errors = new ArrayList<>();
        try {
            IamIds.requireVersion(input.expectedVersion(), current.version());
        } catch (BizException exception) {
            errors.add(new ValidationIssue("expectedVersion", IamReasonCode.REVISION_CONFLICT, exception.getMessage()));
        }
        List<String> invalid = invalidDelegatedAssignments(domain, actor, IamIds.require(id), input.group().selection());
        if (!invalid.isEmpty()) {
            errors.add(new ValidationIssue("selection", IamReasonCode.POLICY_CONFLICT, "组变更后委派派生授权不再成立"));
        }
        List<String> affected = assignmentIds(domain, actor, IamIds.require(id));
        ReferenceImpactPreview result = new ReferenceImpactPreview(affected,
                new ImpactSummary(null, (long) affected.size(), null, false));
        return new Preview<>(current.version(), errors.isEmpty(), errors, List.of(), result.impactSummary(), result);
    }

    private ResourceDetail<GroupRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        List<ResourceDetail<GroupRecord>> rows = domain == AuthorizationDomain.PLATFORM
                ? jdbc.query("SELECT id,name,description,version FROM iam_platform_group WHERE id=:id",
                Map.of("id", id), (row, index) -> detail(domain, actor, row.getLong("id"), row.getString("name"),
                        row.getString("description"), row.getString("version")))
                : jdbc.query("""
                SELECT id,name,description,version FROM iam_tenant_group WHERE tenant_id=:tenantId AND id=:id
                """, domainParameters(domain, actor, id), (row, index) -> detail(domain, actor, row.getLong("id"),
                        row.getString("name"), row.getString("description"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<GroupRecord> lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        List<ResourceDetail<GroupRecord>> rows = domain == AuthorizationDomain.PLATFORM
                ? jdbc.query("SELECT id,name,description,version FROM iam_platform_group WHERE id=:id FOR UPDATE",
                Map.of("id", id), (row, index) -> detail(domain, actor, row.getLong("id"), row.getString("name"),
                        row.getString("description"), row.getString("version")))
                : jdbc.query("""
                SELECT id,name,description,version FROM iam_tenant_group
                 WHERE tenant_id=:tenantId AND id=:id FOR UPDATE
                """, domainParameters(domain, actor, id), (row, index) -> detail(domain, actor, row.getLong("id"),
                        row.getString("name"), row.getString("description"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<GroupRecord> detail(AuthorizationDomain domain, ActiveIdentity actor, long id, String name,
                                               String description, String version) {
        Selection selection = selectionOf(domain, actor, id);
        long count = selection.members().size();
        return IamDetails.of(new GroupRecord(IamIds.text(id), name, description, selection, count), version);
    }

    private Selection selectionOf(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        if (domain == AuthorizationDomain.PLATFORM) {
            List<String> members = jdbc.queryForList(
                    "SELECT member_id FROM iam_platform_group_member WHERE group_id=:id ORDER BY member_id",
                    Map.of("id", id), String.class);
            return new Selection(members, List.of());
        }
        Map<String, Object> parameters = domainParameters(domain, actor, id);
        List<String> members = jdbc.queryForList("""
                SELECT member_id FROM iam_tenant_group_member WHERE tenant_id=:tenantId AND group_id=:id
                 ORDER BY member_id
                """, parameters, String.class);
        List<DepartmentSelection> departments = jdbc.query("""
                SELECT department_id,include_descendants FROM iam_tenant_group_department
                 WHERE tenant_id=:tenantId AND group_id=:id ORDER BY department_id
                """, parameters, (row, index) -> new DepartmentSelection(row.getString("department_id"),
                row.getBoolean("include_descendants")));
        return new Selection(members, departments);
    }

    private void insertGroup(AuthorizationDomain domain, ActiveIdentity actor, long id, GroupDraft input) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("name", input.name());
        parameters.put("description", input.description());
        if (domain == AuthorizationDomain.PLATFORM) {
            jdbc.update("INSERT INTO iam_platform_group(id,name,description) VALUES (:id,:name,:description)",
                    parameters);
            return;
        }
        parameters.put("tenantId", IamIds.require(actor.context().tenantId()));
        jdbc.update("""
                INSERT INTO iam_tenant_group(id,tenant_id,name,description)
                VALUES (:id,:tenantId,:name,:description)
                """, parameters);
    }

    private void updateGroup(AuthorizationDomain domain, ActiveIdentity actor, long id, GroupDraft input) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("name", input.name());
        parameters.put("description", input.description());
        if (domain == AuthorizationDomain.PLATFORM) {
            jdbc.update("""
                    UPDATE iam_platform_group SET name=:name,description=:description,version=version+1 WHERE id=:id
                    """, parameters);
            return;
        }
        parameters.put("tenantId", IamIds.require(actor.context().tenantId()));
        jdbc.update("""
                UPDATE iam_tenant_group SET name=:name,description=:description,version=version+1
                 WHERE tenant_id=:tenantId AND id=:id
                """, parameters);
    }

    private void replaceSelection(AuthorizationDomain domain, ActiveIdentity actor, long id, Selection selection) {
        requireMembers(domain, actor, selection.members());
        requireDepartments(domain, actor, selection.departments());
        clearSelection(domain, actor, id);
        if (domain == AuthorizationDomain.PLATFORM) {
            for (String memberId : selection.members()) {
                jdbc.update("INSERT INTO iam_platform_group_member(group_id,member_id) VALUES (:id,:memberId)",
                        Map.of("id", id, "memberId", IamIds.require(memberId)));
            }
            return;
        }
        for (String memberId : selection.members()) {
            Map<String, Object> parameters = domainParameters(domain, actor, id);
            parameters.put("memberId", IamIds.require(memberId));
            jdbc.update("""
                    INSERT INTO iam_tenant_group_member(tenant_id,group_id,member_id)
                    VALUES (:tenantId,:id,:memberId)
                    """, parameters);
        }
        for (DepartmentSelection department : selection.departments()) {
            Map<String, Object> parameters = domainParameters(domain, actor, id);
            parameters.put("departmentId", IamIds.require(department.id()));
            parameters.put("descendants", department.includeDescendants());
            jdbc.update("""
                    INSERT INTO iam_tenant_group_department(tenant_id,group_id,department_id,include_descendants)
                    VALUES (:tenantId,:id,:departmentId,:descendants)
                    """, parameters);
        }
    }

    private void clearSelection(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        if (domain == AuthorizationDomain.PLATFORM) {
            jdbc.update("DELETE FROM iam_platform_group_member WHERE group_id=:id", Map.of("id", id));
            return;
        }
        Map<String, Object> parameters = domainParameters(domain, actor, id);
        jdbc.update("DELETE FROM iam_tenant_group_member WHERE tenant_id=:tenantId AND group_id=:id", parameters);
        jdbc.update("DELETE FROM iam_tenant_group_department WHERE tenant_id=:tenantId AND group_id=:id", parameters);
    }

    private void requireMembers(AuthorizationDomain domain, ActiveIdentity actor, List<String> members) {
        for (String memberId : members) {
            long id = IamIds.require(memberId);
            Long count = domain == AuthorizationDomain.PLATFORM
                    ? jdbc.queryForObject("SELECT COUNT(*) FROM iam_platform_member WHERE id=:id AND status<>'REMOVED'",
                    Map.of("id", id), Long.class)
                    : jdbc.queryForObject("""
                    SELECT COUNT(*) FROM iam_tenant_member
                     WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                    """, Map.of("tenantId", IamIds.require(actor.context().tenantId()), "id", id), Long.class);
            if (count == null || count == 0) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
    }

    private void requireDepartments(AuthorizationDomain domain, ActiveIdentity actor,
                                    List<DepartmentSelection> departments) {
        if (domain == AuthorizationDomain.PLATFORM && !departments.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        for (DepartmentSelection department : departments) {
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_department WHERE tenant_id=:tenantId AND id=:id",
                    Map.of("tenantId", IamIds.require(actor.context().tenantId()),
                            "id", IamIds.require(department.id())), Long.class);
            if (count == null || count == 0) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
    }

    private boolean referenced(AuthorizationDomain domain, ActiveIdentity actor, long groupId) {
        Map<String, Object> parameters = domain == AuthorizationDomain.PLATFORM
                ? Map.of("id", groupId, "group", SubjectType.GROUP.name())
                : Map.of("id", groupId, "tenantId", IamIds.require(actor.context().tenantId()),
                "group", SubjectType.GROUP.name());
        Long assignments = jdbc.queryForObject(domain == AuthorizationDomain.PLATFORM
                ? "SELECT COUNT(*) FROM iam_role_assignment WHERE platform_group_id=:id AND subject_type=:group"
                : """
                SELECT COUNT(*) FROM iam_role_assignment
                 WHERE tenant_id=:tenantId AND tenant_group_id=:id AND subject_type=:group
                """, parameters, Long.class);
        return assignments != null && assignments > 0;
    }

    private List<String> assignmentIds(AuthorizationDomain domain, ActiveIdentity actor, long groupId) {
        Map<String, Object> parameters = domain == AuthorizationDomain.PLATFORM
                ? Map.of("id", groupId, "group", SubjectType.GROUP.name(), "active", GrantStatus.ACTIVE.name())
                : Map.of("id", groupId, "tenantId", IamIds.require(actor.context().tenantId()),
                "group", SubjectType.GROUP.name(), "active", GrantStatus.ACTIVE.name());
        return jdbc.queryForList(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT id FROM iam_role_assignment
                 WHERE platform_group_id=:id AND subject_type=:group AND status=:active
                """
                : """
                SELECT id FROM iam_role_assignment
                 WHERE tenant_id=:tenantId AND tenant_group_id=:id AND subject_type=:group AND status=:active
                """, parameters, String.class);
    }

    private List<String> invalidDelegatedAssignments(AuthorizationDomain domain, ActiveIdentity actor, long groupId,
                                                     Selection next) {
        List<String> invalid = new ArrayList<>();
        Map<String, Object> parameters = domain == AuthorizationDomain.PLATFORM
                ? Map.of("id", groupId, "group", SubjectType.GROUP.name(), "active", GrantStatus.ACTIVE.name())
                : Map.of("id", groupId, "tenantId", IamIds.require(actor.context().tenantId()),
                "group", SubjectType.GROUP.name(), "active", GrantStatus.ACTIVE.name());
        List<Long> delegated = jdbc.queryForList(domain == AuthorizationDomain.PLATFORM
                ? """
                SELECT id FROM iam_role_assignment
                 WHERE platform_group_id=:id AND subject_type=:group AND status=:active
                   AND delegation_grant_id IS NOT NULL
                """
                : """
                SELECT id FROM iam_role_assignment
                 WHERE tenant_id=:tenantId AND tenant_group_id=:id AND subject_type=:group AND status=:active
                   AND delegation_grant_id IS NOT NULL
                """, parameters, Long.class);
        if (!delegated.isEmpty() && next.members().isEmpty() && next.departments().isEmpty()) {
            delegated.forEach(id -> invalid.add(IamIds.text(id)));
        }
        return invalid;
    }

    private Map<String, Object> domainParameters(AuthorizationDomain domain, ActiveIdentity actor) {
        Map<String, Object> parameters = new HashMap<>();
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

    private static IamAction action(AuthorizationDomain domain, AccessKind kind) {
        return switch (kind) {
            case READ -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_READ
                    : IamAction.TENANT_GROUP_READ;
            case CREATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_CREATE
                    : IamAction.TENANT_GROUP_CREATE;
            case UPDATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_UPDATE
                    : IamAction.TENANT_GROUP_UPDATE;
            case DELETE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_DELETE
                    : IamAction.TENANT_GROUP_DELETE;
            case PREVIEW -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_PREVIEW
                    : IamAction.TENANT_GROUP_PREVIEW;
        };
    }

    private enum AccessKind {
        READ, CREATE, UPDATE, DELETE, PREVIEW
    }
}
