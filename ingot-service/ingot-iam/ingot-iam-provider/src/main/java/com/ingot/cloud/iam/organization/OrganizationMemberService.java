package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.*;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.persistence.*;
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
