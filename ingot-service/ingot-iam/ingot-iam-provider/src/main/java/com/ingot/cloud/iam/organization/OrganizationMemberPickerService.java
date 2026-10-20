package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import com.ingot.cloud.iam.assignment.TenantLiveAssignmentService;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.MemberPickerBranch;
import com.ingot.cloud.iam.persistence.OrganizationMemberRepository;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>组织成员对话框的独立浏览投影，组合导航与场景候选而不产生任职或授权。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class OrganizationMemberPickerService {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final DepartmentNavigationService navigation;
    private final DepartmentQueryRepository departments;
    private final OrganizationMemberRepository repository;
    private final OrganizationMemberService members;
    private final DepartmentLeaderService leaders;
    private final TenantLiveAssignmentService assignments;

    /** 各场景沿用原资格，根归属在计数分页前求交；搜索始终在该场景候选池内进行。 */
    public OrganizationMemberPickerPage browse(OrganizationMemberPickerScene scene, String departmentId, String keyword,
            String targetDepartmentId, String delegationGrantId, int page, int size, int departmentPage, int departmentSize) {
        IamPages.require(page, size);
        IamPages.require(departmentPage, departmentSize);
        if (scene == null || scene == OrganizationMemberPickerScene.DEPARTMENT_LEADER && blank(targetDepartmentId)
                || scene != OrganizationMemberPickerScene.DEPARTMENT_LEADER && !blank(targetDepartmentId)
                || scene != OrganizationMemberPickerScene.ASSIGNMENT && !blank(delegationGrantId))
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var actor = access.requireCurrent();
        if (actor.context().domain() != AuthorizationDomain.TENANT) throw new BizException(IamReasonCode.ACTION_DENIED);
        long tenant = IamIds.require(actor.context().tenantId());
        boolean canNavigate = access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ, false);
        DepartmentNavigationPage nav;
        MemberPickerBranch branch;
        if (canNavigate) {
            nav = navigation.page(departmentId, departmentPage, departmentSize);
            if (!blank(departmentId)) {
                // 即使搜索忽略分支，也验证客户端提交的部门不可越界。
                scopes.requireVisibleDepartment(actor.context(), IamAction.TENANT_DEPARTMENT_READ, IamIds.require(departmentId));
                var row = departments.find(tenant, IamIds.require(departmentId));
                if (row == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                branch = MemberPickerBranch.department(IamIds.require(departmentId), row.getParentId() == null);
            } else branch = nav.organization() == null ? MemberPickerBranch.none()
                    : MemberPickerBranch.department(IamIds.require(nav.organization().id()), true);
        } else {
            if (!blank(departmentId)) throw new BizException(IamReasonCode.ACTION_DENIED);
            nav = new DepartmentNavigationPage(List.of(), 0, departmentPage, departmentSize, List.of(), null);
            branch = MemberPickerBranch.all();
        }
        if (!blank(keyword)) branch = MemberPickerBranch.all();
        PageResponse<OrganizationMemberPickerOption> result = switch (scene) {
            case MEMBER -> {
                var data = members.pickerPage(keyword, page, size, branch);
                yield new PageResponse<>(data.items().stream().map(row -> new OrganizationMemberPickerOption(
                        row.id(), row.displayName(), row.status(), row.status() == MemberStatus.ACTIVE, row.departments(), null,
                        row.avatar())).toList(),
                        data.total(), data.page(), data.pageSize());
            }
            case DEPARTMENT_LEADER -> {
                var data = leaders.pickerCandidates(targetDepartmentId, keyword, page, size, branch);
                yield new PageResponse<>(data.items().stream().map(row -> new OrganizationMemberPickerOption(
                        row.memberId(), row.displayName(), row.status(), row.available(), row.departments(), null,
                        row.avatar())).toList(),
                        data.total(), data.page(), data.pageSize());
            }
            case ASSIGNMENT -> {
                var data = assignments.pickerOptions(delegationGrantId, keyword, page, size, branch);
                yield new PageResponse<>(data.items().stream().map(row -> new OrganizationMemberPickerOption(
                        row.id(), row.name(), MemberStatus.ACTIVE, true, row.departments(), null)).toList(),
                        data.total(), data.page(), data.pageSize());
            }
        };
        Set<BigInteger> unassigned = Set.of();
        // 仅真实零关系且成员/根均可读时返回虚拟根标签；不可见关系不等于未分配。
        if (nav.organization() != null && access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)) {
            var memberScope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
            var readableIds = result.items().stream().map(row -> BigInteger.valueOf(IamIds.require(row.id())))
                    .toList();
            unassigned = repository.unassigned(tenant, readableIds, memberScope);
        }
        var root = nav.organization();
        var implicit = unassigned;
        return new OrganizationMemberPickerPage(canNavigate, nav, new PageResponse<>(result.items().stream().map(row ->
                new OrganizationMemberPickerOption(row.id(), row.name(), row.status(), row.available(), row.departments(),
                        implicit.contains(BigInteger.valueOf(IamIds.require(row.id()))) ? root : null, row.avatar())).toList(),
                result.total(), page, size));
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
