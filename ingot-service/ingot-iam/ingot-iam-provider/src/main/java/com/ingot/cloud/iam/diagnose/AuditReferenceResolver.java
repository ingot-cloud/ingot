package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.AuditReferenceMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.AuditReferenceRows;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.policy.FieldPolicySnapshot;
import com.ingot.framework.commons.model.iam.AuditEntry;
import com.ingot.framework.commons.model.iam.AuditReference;
import com.ingot.framework.commons.model.iam.AuditReferenceState;
import com.ingot.framework.commons.model.iam.AuditReferenceType;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.SubjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>按查看人的读取权限、对象范围和字段策略，批量解析一页审计涉及的对象名称。</p>
 *
 * <p>每种对象类型至多一次批量查询（成员与部门另加一次范围查询），与页内条数无关；
 * 无权读取的对象只返回 {@link AuditReferenceState#HIDDEN}，不披露名称。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class AuditReferenceResolver {
    /** 组合名称的分隔符。 */
    static final String NAME_JOINER = " · ";
    /** 组合名称中无权查看的部分。 */
    static final String HIDDEN_PART = "无权查看";
    /** 组合名称中已删除的部分。 */
    static final String DELETED_PART = "已删除";
    /** 没有版本名称时的版本显示。 */
    static final String REVISION_FALLBACK = "第%s版";

    private final AuditReferenceMapper references;
    private final IamTenantMemberMapper tenantMembers;
    private final IamPlatformMemberMapper platformMembers;
    private final IamDepartmentMapper departments;
    private final ResourceAccess scopes;
    private final FieldAccessEvaluator fieldAccess;

    /**
     * 解析一页审计的引用字典。
     *
     * @param actor 可信查看人
     * @param view 查看人当前授权视图
     * @param entries 本页审计，按页内顺序
     * @return 以 {@code type:id} 为键的引用
     */
    public Map<String, AuditReference> resolve(ActiveIdentity actor, AuthorizationEvaluator.AuthorizationView view,
                                               List<AuditEntry> entries) {
        AuditReferenceCollector collector = new AuditReferenceCollector();
        entries.forEach(collector::collect);
        Page page = new Page(actor, view, collector);
        page.expand();
        page.resolveNames();
        page.compose();
        page.applySnapshots();
        return page.result;
    }

    private final class Page {
        private final ActiveIdentity actor;
        private final AuthorizationEvaluator.AuthorizationView view;
        private final AuditReferenceCollector collector;
        private final AuthorizationDomain domain;
        private final BigInteger tenantId;
        private final Map<String, AuditReference> result = new LinkedHashMap<>();
        private final Map<BigInteger, AuditReferenceRows.Assignment> assignments = new HashMap<>();
        private final Map<BigInteger, AuditReferenceRows.Delegation> delegations = new HashMap<>();
        private final Map<BigInteger, AuditReferenceRows.Task> tasks = new HashMap<>();
        private final Map<BigInteger, BigInteger> accountMembers = new HashMap<>();

        private Page(ActiveIdentity actor, AuthorizationEvaluator.AuthorizationView view,
                     AuditReferenceCollector collector) {
            this.actor = actor;
            this.view = view;
            this.collector = collector;
            this.domain = actor.context().domain();
            this.tenantId = domain == AuthorizationDomain.TENANT ? new BigInteger(actor.context().tenantId()) : null;
        }

        private void expand() {
            Set<BigInteger> assignmentIds = ids(AuditReferenceType.ASSIGNMENT);
            if (!assignmentIds.isEmpty() && has(IamAction.PLATFORM_ASSIGNMENT_READ, IamAction.TENANT_ASSIGNMENT_READ)) {
                for (AuditReferenceRows.Assignment row : references.assignments(tenantId, assignmentIds)) {
                    assignments.put(row.id(), row);
                    collector.add(AuditReferenceType.ROLE, text(row.roleId()));
                    collector.add(row.subjectType() == SubjectType.GROUP ? AuditReferenceType.GROUP
                            : AuditReferenceType.MEMBER, text(row.subjectType() == SubjectType.GROUP
                            ? row.groupId() : row.memberId()));
                }
            }
            Set<BigInteger> delegationIds = ids(AuditReferenceType.DELEGATION);
            if (!delegationIds.isEmpty() && has(IamAction.PLATFORM_DELEGATION_READ, IamAction.TENANT_DELEGATION_READ)) {
                for (AuditReferenceRows.Delegation row : references.delegations(tenantId, delegationIds)) {
                    delegations.put(row.id(), row);
                    collector.add(AuditReferenceType.MEMBER, text(row.memberId()));
                }
            }
            Set<BigInteger> taskIds = ids(AuditReferenceType.ROLE_APPLICATION_TASK);
            if (!taskIds.isEmpty() && roleReadable()) {
                for (AuditReferenceRows.Task row : references.tasks(tenantId, taskIds)) {
                    tasks.put(row.id(), row);
                    collector.add(AuditReferenceType.ROLE_REVISION, text(row.revisionId()));
                }
            }
            Set<BigInteger> accountIds = ids(AuditReferenceType.ACCOUNT);
            if (!accountIds.isEmpty() && domain == AuthorizationDomain.TENANT) {
                for (AuditReferenceRows.AccountMember row : references.tenantAccountMembers(tenantId, accountIds)) {
                    accountMembers.put(row.accountId(), row.memberId());
                    collector.add(AuditReferenceType.MEMBER, text(row.memberId()));
                }
            }
        }

        private void resolveNames() {
            members();
            departments();
            simple(AuditReferenceType.GROUP, has(IamAction.PLATFORM_GROUP_READ, IamAction.TENANT_GROUP_READ),
                    ids -> references.groups(tenantId, ids));
            simple(AuditReferenceType.ROLE, roleReadable(), ids -> references.roles(tenantId, ids));
            revisions();
            simple(AuditReferenceType.APPLICATION,
                    has(IamAction.PLATFORM_APPLICATION_READ, IamAction.TENANT_APPLICATION_READ),
                    ids -> references.applications(domain.name(), ids));
            tenants();
            if (domain == AuthorizationDomain.PLATFORM) {
                simple(AuditReferenceType.ACCOUNT, has(IamAction.PLATFORM_ACCOUNT_READ, null),
                        references::accounts);
            }
        }

        private void members() {
            Set<BigInteger> ids = ids(AuditReferenceType.MEMBER);
            if (ids.isEmpty()) {
                return;
            }
            IamAction read = domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ;
            if (!view.actionCodes().contains(read.getCode())) {
                hideAll(AuditReferenceType.MEMBER, ids);
                return;
            }
            Map<BigInteger, String> names = new HashMap<>();
            Set<BigInteger> active = new HashSet<>();
            Set<BigInteger> visible = new HashSet<>();
            ObjectScope scope = scopes.memberRead(actor.context(), read);
            if (domain == AuthorizationDomain.PLATFORM) {
                for (IamPlatformMemberEntity row : platformMembers.selectList(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                        .select(IamPlatformMemberEntity::getId, IamPlatformMemberEntity::getDisplayName,
                                IamPlatformMemberEntity::getStatus)
                        .in(IamPlatformMemberEntity::getId, ids))) {
                    names.put(row.getId(), row.getDisplayName());
                    if (row.getStatus() != MemberStatus.REMOVED) {
                        active.add(row.getId());
                    }
                }
                if (!scope.coversNone() && !active.isEmpty()) {
                    var wrapper = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                            .select(IamPlatformMemberEntity::getId).in(IamPlatformMemberEntity::getId, active);
                    ObjectScopeSql.restrictPlatformMembers(wrapper, scope);
                    platformMembers.selectList(wrapper).forEach(row -> visible.add(row.getId()));
                }
            } else {
                for (IamTenantMemberEntity row : tenantMembers.selectList(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                        .select(IamTenantMemberEntity::getId, IamTenantMemberEntity::getDisplayName,
                                IamTenantMemberEntity::getStatus)
                        .eq(IamTenantMemberEntity::getTenantId, tenantId)
                        .in(IamTenantMemberEntity::getId, ids))) {
                    names.put(row.getId(), row.getDisplayName());
                    if (row.getStatus() != MemberStatus.REMOVED) {
                        active.add(row.getId());
                    }
                }
                if (!scope.coversNone() && !active.isEmpty()) {
                    var wrapper = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                            .select(IamTenantMemberEntity::getId)
                            .eq(IamTenantMemberEntity::getTenantId, tenantId)
                            .in(IamTenantMemberEntity::getId, active);
                    ObjectScopeSql.restrictTenantMembers(wrapper, scope, tenantId);
                    tenantMembers.selectList(wrapper).forEach(row -> visible.add(row.getId()));
                }
            }
            FieldPolicySnapshot snapshot = domain == AuthorizationDomain.TENANT && !visible.isEmpty()
                    ? fieldAccess.snapshot(tenantId.longValueExact(), PolicyScenario.MANAGEMENT) : null;
            long viewer = Long.parseLong(actor.context().memberId());
            for (BigInteger id : ids) {
                String key = AuditReferenceType.MEMBER.key(text(id));
                if (!names.containsKey(id) || !active.contains(id)) {
                    result.put(key, AuditReference.deleted(AuditReferenceType.MEMBER, text(id), null));
                } else if (!visible.contains(id)) {
                    result.put(key, AuditReference.hidden(AuditReferenceType.MEMBER, text(id)));
                } else {
                    String name = snapshot == null ? names.get(id) : FieldProjection.project(names.get(id),
                            fieldAccess.access(snapshot, viewer, id.longValueExact(),
                                    MemberFieldKey.VALUE_DISPLAY_NAME).visibility());
                    result.put(key, name == null ? AuditReference.hidden(AuditReferenceType.MEMBER, text(id))
                            : AuditReference.visible(AuditReferenceType.MEMBER, text(id), name));
                }
            }
        }

        private void departments() {
            Set<BigInteger> ids = ids(AuditReferenceType.DEPARTMENT);
            if (ids.isEmpty()) {
                return;
            }
            if (domain != AuthorizationDomain.TENANT
                    || !view.actionCodes().contains(IamAction.TENANT_DEPARTMENT_READ.getCode())) {
                hideAll(AuditReferenceType.DEPARTMENT, ids);
                return;
            }
            Map<BigInteger, String> names = new HashMap<>();
            for (IamDepartmentEntity row : departments.selectList(Wrappers.<IamDepartmentEntity>lambdaQuery()
                    .select(IamDepartmentEntity::getId, IamDepartmentEntity::getName)
                    .eq(IamDepartmentEntity::getTenantId, tenantId)
                    .in(IamDepartmentEntity::getId, ids))) {
                names.put(row.getId(), row.getName());
            }
            Set<BigInteger> visible = new HashSet<>();
            ObjectScope scope = scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ);
            if (!scope.coversNone() && !names.isEmpty()) {
                var wrapper = Wrappers.<IamDepartmentEntity>lambdaQuery()
                        .select(IamDepartmentEntity::getId)
                        .eq(IamDepartmentEntity::getTenantId, tenantId)
                        .in(IamDepartmentEntity::getId, names.keySet());
                ObjectScopeSql.restrictDepartments(wrapper, scope);
                departments.selectList(wrapper).forEach(row -> visible.add(row.getId()));
            }
            for (BigInteger id : ids) {
                String key = AuditReferenceType.DEPARTMENT.key(text(id));
                if (!names.containsKey(id)) {
                    result.put(key, AuditReference.deleted(AuditReferenceType.DEPARTMENT, text(id), null));
                } else if (!visible.contains(id)) {
                    result.put(key, AuditReference.hidden(AuditReferenceType.DEPARTMENT, text(id)));
                } else {
                    result.put(key, AuditReference.visible(AuditReferenceType.DEPARTMENT, text(id), names.get(id)));
                }
            }
        }

        private void revisions() {
            Set<BigInteger> ids = ids(AuditReferenceType.ROLE_REVISION);
            if (ids.isEmpty()) {
                return;
            }
            if (!roleReadable()) {
                hideAll(AuditReferenceType.ROLE_REVISION, ids);
                return;
            }
            Map<BigInteger, String> names = new HashMap<>();
            for (AuditReferenceRows.Revision row : references.revisions(tenantId, ids)) {
                String version = row.versionName() == null || row.versionName().isBlank()
                        ? REVISION_FALLBACK.formatted(row.revision()) : row.versionName();
                names.put(row.id(), row.roleName() + NAME_JOINER + version);
            }
            put(AuditReferenceType.ROLE_REVISION, ids, names);
        }

        private void tenants() {
            Set<BigInteger> ids = ids(AuditReferenceType.TENANT);
            if (ids.isEmpty()) {
                return;
            }
            if (domain == AuthorizationDomain.TENANT) {
                Set<BigInteger> own = new HashSet<>();
                Set<BigInteger> others = new HashSet<>();
                ids.forEach(id -> (id.equals(tenantId) ? own : others).add(id));
                hideAll(AuditReferenceType.TENANT, others);
                if (!own.isEmpty()) {
                    put(AuditReferenceType.TENANT, own, names(references.tenants(own)));
                }
                return;
            }
            simple(AuditReferenceType.TENANT, has(IamAction.PLATFORM_TENANT_READ, null), references::tenants);
        }

        private void compose() {
            for (String id : collector.ids(AuditReferenceType.ASSIGNMENT)) {
                AuditReferenceRows.Assignment row = assignments.get(new BigInteger(id));
                if (!has(IamAction.PLATFORM_ASSIGNMENT_READ, IamAction.TENANT_ASSIGNMENT_READ)) {
                    hide(AuditReferenceType.ASSIGNMENT, id);
                } else if (row == null) {
                    deleted(AuditReferenceType.ASSIGNMENT, id);
                } else {
                    AuditReferenceType subject = row.subjectType() == SubjectType.GROUP
                            ? AuditReferenceType.GROUP : AuditReferenceType.MEMBER;
                    String subjectId = text(row.subjectType() == SubjectType.GROUP ? row.groupId() : row.memberId());
                    visible(AuditReferenceType.ASSIGNMENT, id,
                            part(AuditReferenceType.ROLE, text(row.roleId())) + NAME_JOINER + part(subject, subjectId));
                }
            }
            for (String id : collector.ids(AuditReferenceType.DELEGATION)) {
                AuditReferenceRows.Delegation row = delegations.get(new BigInteger(id));
                if (!has(IamAction.PLATFORM_DELEGATION_READ, IamAction.TENANT_DELEGATION_READ)) {
                    hide(AuditReferenceType.DELEGATION, id);
                } else if (row == null) {
                    deleted(AuditReferenceType.DELEGATION, id);
                } else {
                    visible(AuditReferenceType.DELEGATION, id, part(AuditReferenceType.MEMBER, text(row.memberId())));
                }
            }
            for (String id : collector.ids(AuditReferenceType.ROLE_APPLICATION_TASK)) {
                AuditReferenceRows.Task row = tasks.get(new BigInteger(id));
                if (!roleReadable()) {
                    hide(AuditReferenceType.ROLE_APPLICATION_TASK, id);
                } else if (row == null) {
                    deleted(AuditReferenceType.ROLE_APPLICATION_TASK, id);
                } else {
                    visible(AuditReferenceType.ROLE_APPLICATION_TASK, id,
                            part(AuditReferenceType.ROLE_REVISION, text(row.revisionId())));
                }
            }
            if (domain == AuthorizationDomain.TENANT) {
                for (String id : collector.ids(AuditReferenceType.ACCOUNT)) {
                    BigInteger member = accountMembers.get(new BigInteger(id));
                    AuditReference resolved = member == null ? null
                            : result.get(AuditReferenceType.MEMBER.key(text(member)));
                    result.put(AuditReferenceType.ACCOUNT.key(id), resolved == null
                            ? AuditReference.deleted(AuditReferenceType.ACCOUNT, id, null)
                            : new AuditReference(AuditReferenceType.ACCOUNT, id, resolved.state(), resolved.name()));
                }
            }
        }

        private void applySnapshots() {
            collector.snapshots().forEach((key, name) -> {
                AuditReference current = result.get(key);
                if (current != null && current.state() == AuditReferenceState.DELETED && current.name() == null) {
                    result.put(key, AuditReference.deleted(current.type(), current.id(), name));
                }
            });
        }

        private void simple(AuditReferenceType type, boolean readable,
                            Function<Set<BigInteger>, List<AuditReferenceRows.Name>> loader) {
            Set<BigInteger> ids = ids(type);
            if (ids.isEmpty()) {
                return;
            }
            if (!readable) {
                hideAll(type, ids);
                return;
            }
            put(type, ids, names(loader.apply(ids)));
        }

        private void put(AuditReferenceType type, Set<BigInteger> ids, Map<BigInteger, String> names) {
            for (BigInteger id : ids) {
                String name = names.get(id);
                result.put(type.key(text(id)), name == null ? AuditReference.deleted(type, text(id), null)
                        : AuditReference.visible(type, text(id), name));
            }
        }

        private String part(AuditReferenceType type, String id) {
            AuditReference reference = id == null ? null : result.get(type.key(id));
            if (reference == null || reference.state() == AuditReferenceState.HIDDEN) {
                return HIDDEN_PART;
            }
            return reference.state() == AuditReferenceState.DELETED ? DELETED_PART : reference.name();
        }

        private void hideAll(AuditReferenceType type, Set<BigInteger> ids) {
            ids.forEach(id -> hide(type, text(id)));
        }

        private void hide(AuditReferenceType type, String id) {
            result.put(type.key(id), AuditReference.hidden(type, id));
        }

        private void deleted(AuditReferenceType type, String id) {
            result.put(type.key(id), AuditReference.deleted(type, id, null));
        }

        private void visible(AuditReferenceType type, String id, String name) {
            result.put(type.key(id), AuditReference.visible(type, id, name));
        }

        private boolean roleReadable() {
            return domain == AuthorizationDomain.PLATFORM
                    ? view.actionCodes().contains(IamAction.PLATFORM_ROLE_READ.getCode())
                    || view.actionCodes().contains(IamAction.PLATFORM_SHARED_ROLE_READ.getCode())
                    : view.actionCodes().contains(IamAction.TENANT_ROLE_READ.getCode());
        }

        private boolean has(IamAction platform, IamAction tenant) {
            IamAction action = domain == AuthorizationDomain.PLATFORM ? platform : tenant;
            return action != null && view.actionCodes().contains(action.getCode());
        }

        private Set<BigInteger> ids(AuditReferenceType type) {
            Set<BigInteger> ids = new LinkedHashSet<>();
            collector.ids(type).forEach(id -> ids.add(new BigInteger(id)));
            return ids;
        }
    }

    private static Map<BigInteger, String> names(List<AuditReferenceRows.Name> rows) {
        Map<BigInteger, String> names = new HashMap<>();
        rows.forEach(row -> names.put(row.id(), row.name()));
        return names;
    }

    private static String text(BigInteger value) {
        return value == null ? null : value.toString();
    }
}
