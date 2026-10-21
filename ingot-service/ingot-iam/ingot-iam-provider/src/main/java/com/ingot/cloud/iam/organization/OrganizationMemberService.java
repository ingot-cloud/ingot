package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.*;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupDepartmentEntity;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>供组织业务复用的最小成员候选，沿用成员读取资格而不扩大数据或字段范围。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class OrganizationMemberService {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final OrganizationMemberRepository candidates;
    private final OrganizationMemberDepartments memberDepartments;
    private final FieldAccessEvaluator fields;
    private final GroupRepository groups;
    private final DepartmentClosure closure;
    private final DepartmentQueryRepository departments;
    private final MemberQueryRepository memberQueries;

    /** 在同一权限过滤下搜索、分页及少量ID回显；不返回联系方式。 */
    public PageResponse<MemberRecord> page(String departmentId, String keyword, List<String> ids, int page, int size) {
        return page(departmentId, keyword, ids, page, size, null);
    }

    /** 按可读组的直接成员关联回显，不返回无权成员。 */
    public PageResponse<MemberRecord> groupMembers(String groupId, int page, int size) {
        var actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_GROUP_READ);
        scopes.requireVisibleObject(actor.context(), IamAction.TENANT_GROUP_READ, IamIds.require(groupId));
        return page(null, null, List.of(), page, size, IamIds.require(groupId));
    }

    /**
     * 分页列出组的有效成员，展开规则与人数统计一致，并标出直接绑定与部门来源。
     *
     * @param groupId 组 ID
     * @param page 页码
     * @param size 页大小
     * @param name 显示名包含匹配，可空
     * @return 有效成员页
     */
    public PageResponse<GroupEffectiveMemberRecord> effectiveMembers(String groupId, int page, int size, String name) {
        IamPages.require(page, size);
        var actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_GROUP_READ);
        long id = IamIds.require(groupId);
        scopes.requireVisibleObject(actor.context(), IamAction.TENANT_GROUP_READ, id);
        long tenant = IamIds.require(actor.context().tenantId());
        long viewer = IamIds.require(actor.context().memberId());
        if (groups.findTenant(tenant, id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        var scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        var snapshot = fields.snapshotFresh(tenant, PolicyScenario.MANAGEMENT);
        fields.requireOriginalLookup(snapshot, viewer, MemberFieldKey.VALUE_DISPLAY_NAME, name, scope);
        var result = candidates.pageEffective(tenant, scope, name, id, page, size);
        var memberIds = result.getRecords().stream().map(row -> row.getId()).toList();
        var views = memberDepartments.visible(actor, memberIds);
        var memberships = membershipsOf(actor, tenant, id, memberIds);
        var batch = fields.prepare(snapshot, viewer, memberIds.stream().map(BigInteger::longValueExact).toList());
        List<GroupEffectiveMemberRecord> items = result.getRecords().stream().map(row -> {
            var visibleDepartments = views.getOrDefault(row.getId(), List.of());
            var raw = new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(), row.getPhone(),
                    row.getEmail(), null, row.getStatus(), visibleDepartments);
            var projected = fields.project(raw, fields.memberAccess(batch, row.getId().longValueExact()),
                    BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, snapshot.masks());
            return new GroupEffectiveMemberRecord(projected, memberships.get(row.getId()));
        }).toList();
        return IamPages.of(items, result.getTotal(), page, size);
    }

    private Map<BigInteger, GroupMembership> membershipsOf(ActiveIdentity actor,
            long tenant, long groupId, List<BigInteger> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        Set<BigInteger> direct = groups.tenantMemberIdsIn(tenant, groupId, memberIds);
        List<IamTenantGroupDepartmentEntity> links = groups.tenantDepartments(tenant, groupId);
        Map<BigInteger, List<BigInteger>> children = links.stream()
                .anyMatch(row -> Boolean.TRUE.equals(row.getIncludeDescendants()))
                ? closure.loadChildren(BigInteger.valueOf(tenant)) : Map.of();
        List<GroupMembershipSources.Rule> rules = links.stream().map(row -> {
            boolean descendants = Boolean.TRUE.equals(row.getIncludeDescendants());
            return new GroupMembershipSources.Rule(row.getDepartmentId(), descendants,
                    closure.expand(children, List.of(row.getDepartmentId()), descendants));
        }).toList();
        Map<BigInteger, List<BigInteger>> appointments = new LinkedHashMap<>();
        memberQueries.departmentViews(tenant, memberIds).forEach((memberId, values) -> appointments.put(memberId,
                values.stream().map(value -> BigInteger.valueOf(IamIds.require(value.id()))).toList()));
        Map<BigInteger, String> visibleNames = Map.of();
        List<BigInteger> ruleIds = links.stream().map(IamTenantGroupDepartmentEntity::getDepartmentId).distinct()
                .toList();
        if (!ruleIds.isEmpty() && access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ, false)) {
            var allowed = departments.matchingIds(tenant, ruleIds,
                    scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ));
            Map<BigInteger, String> names = new LinkedHashMap<>();
            departments.names(tenant, ruleIds).forEach((departmentId, name) -> {
                if (allowed.contains(departmentId)) {
                    names.put(departmentId, name);
                }
            });
            visibleNames = names;
        }
        return GroupMembershipSources.resolve(memberIds, direct, rules, appointments, visibleNames);
    }

    private PageResponse<MemberRecord> page(String departmentId, String keyword, List<String> ids, int page, int size, Long groupId) {
        return page(departmentId, keyword, ids, page, size, groupId, null);
    }

    /** 使用由专用浏览服务验证的分支，不改变原有读取和字段资格。 */
    public PageResponse<MemberRecord> pickerPage(String keyword, int page, int size, MemberPickerBranch branch) {
        return page(null, keyword, List.of(), page, size, null, branch);
    }

    private PageResponse<MemberRecord> page(String departmentId, String keyword, List<String> ids, int page, int size, Long groupId, MemberPickerBranch branch) {
        IamPages.require(page, size);
        var actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ);
        long tenant = IamIds.require(actor.context().tenantId()), viewer = IamIds.require(actor.context().memberId());
        var selected = ids == null ? List.<BigInteger>of() : ids.stream().map(id -> BigInteger.valueOf(IamIds.require(id))).distinct().toList();
        if (selected.size() > IamPages.MAX_SIZE) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        Long department = departmentId == null || departmentId.isBlank() || keyword != null && !keyword.isBlank() ? null : IamIds.require(departmentId);
        if (department != null) {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
            scopes.requireVisibleDepartment(actor.context(), IamAction.TENANT_DEPARTMENT_READ, department);
        }
        var scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        var snapshot = fields.snapshotFresh(tenant, PolicyScenario.MANAGEMENT);
        fields.requireOriginalLookup(snapshot, viewer, MemberFieldKey.VALUE_DISPLAY_NAME, keyword, scope);
        var result = branch == null ? candidates.page(tenant, scope, department, keyword, selected, page, size, groupId)
                : candidates.page(tenant, scope, department, keyword, selected, page, size, groupId, branch);
        var memberIds = result.getRecords().stream().map(row -> row.getId()).toList();
        var views = memberDepartments.visible(actor, memberIds);
        var batch = fields.prepare(snapshot, viewer, memberIds.stream().map(BigInteger::longValueExact).toList());
        var items = result.getRecords().stream().map(row -> {
            var visibleDepartments = views.getOrDefault(row.getId(), List.of());
            var raw = new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(), null, null, null, row.getStatus(), visibleDepartments);
            return fields.project(raw, fields.memberAccess(batch, row.getId().longValueExact()), BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, snapshot.masks());
        }).toList();
        return new PageResponse<>(items, result.getTotal(), page, size);
    }
}
