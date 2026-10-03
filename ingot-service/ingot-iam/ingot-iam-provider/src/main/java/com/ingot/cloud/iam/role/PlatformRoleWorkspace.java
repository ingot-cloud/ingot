package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.mapper.RoleWorkspaceMapper;
import com.ingot.cloud.iam.persistence.mapper.RoleWorkspaceSql;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>角色工作区只披露当前有效分配，角色、分配及继承组资格分别检查。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformRoleWorkspace {
    private static final TypeReference<List<Long>> REVISION_NUMBERS = new TypeReference<>() { };
    private static final TypeReference<List<SubjectType>> SOURCE_TYPES = new TypeReference<>() { };
    private final IamAccess access;
    private final ResourceAccess resources;
    private final RoleService roles;
    private final AssignmentRepository assignments;
    private final AssignmentService presenter;
    private final RoleWorkspaceMapper mapper;

    /**
     * 查询角色当前有效成员或组，多个固定版本及来源在 SQL 中去重。
     * @param roleId 角色 ID
     * @param revisionId 可选固定版本
     * @param type 成员或组
     * @param keyword 接收主体名称
     * @param page 页码
     * @param pageSize 页大小
     * @return 可见主体与组继承受限提示
     */
    public RoleSubjectPage subjects(String roleId, String revisionId, SubjectType type, String keyword, int page, int pageSize) {
        IamAdmission admission = admission(roleId);
        var groups = visibleGroups(admission.actor());
        var q = query(roleId, revisionId, type, null, keyword, page, pageSize, admission, groups);
        var items = mapper.page(q).stream().map(row -> new RoleSubjectSummary(row.id().toString(), row.name(),
                IamJson.read(row.revisions(), REVISION_NUMBERS).stream().distinct().sorted().toList(),
                IamJson.read(row.sourceTypes(), SOURCE_TYPES).stream().distinct().sorted().toList(), row.sourceCount())).toList();
        return new RoleSubjectPage(items, mapper.count(q), page, pageSize, groups != null);
    }

    /**
     * 查询一个成员对该角色的当前有效来源，调整或撤销必须仍操作实际分配 ID。
     * @param roleId 角色 ID
     * @param memberId 成员 ID
     * @param revisionId 可选版本
     * @param page 页码
     * @param pageSize 页大小
     * @return 分页分配来源与逐条操作能力
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> sources(String roleId, String memberId, String revisionId, int page, int pageSize) {
        IamAdmission admission = admission(roleId);
        var q = query(roleId, revisionId, SubjectType.MEMBER, memberId, null, page, pageSize,
                admission, visibleGroups(admission.actor()));
        return IamPages.details(presenter.presentPlatformRows(admission.actor(), mapper.sources(q)),
                mapper.sourceCount(q), page, pageSize);
    }
    private IamAdmission admission(String roleId) {
        roles.get(AuthorizationDomain.PLATFORM, false, roleId);
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ROLE_READ);
        resources.requireVisibleObject(actor.context(), IamAction.PLATFORM_ROLE_READ, IamIds.require(roleId));
        return access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ);
    }
    private List<BigInteger> visibleGroups(ActiveIdentity actor) {
        if (!access.capabilities(actor, List.of(IamAction.PLATFORM_GROUP_READ)).allows(IamAction.PLATFORM_GROUP_READ, false)) return List.of();
        var scope = resources.objects(actor.context(), IamAction.PLATFORM_GROUP_READ);
        if (scope.coversAll()) return null;
        return scope.clauses().stream().filter(clause -> clause.departmentSets().isEmpty())
                .flatMap(clause -> clause.requiredIds().stream()).distinct().toList();
    }
    private RoleWorkspaceSql.Query query(String roleId, String revisionId, SubjectType type, String memberId,
            String keyword, int page, int pageSize, IamAdmission admission, List<BigInteger> groups) {
        IamPages.require(page, pageSize);
        BigInteger role = BigInteger.valueOf(IamIds.require(roleId));
        BigInteger revision = revisionId == null || revisionId.isBlank() ? null : BigInteger.valueOf(IamIds.require(revisionId));
        if (revision != null) {
            var row = assignments.findRevision(revision.longValueExact());
            if (row == null || !role.equals(row.getRoleId())) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        String name = IamFilters.containsName(keyword);
        String search = name == null ? "%" : "%" + name.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return new RoleWorkspaceSql.Query(role, revision,
                admission.governed() ? null : new BigInteger(admission.actor().context().memberId()), groups, type,
                memberId == null ? null : BigInteger.valueOf(IamIds.require(memberId)), search, Math.multiplyExact(page - 1, pageSize), pageSize, visibleAssignments(admission));
    }
    private List<BigInteger> visibleAssignments(IamAdmission admission) {
        if (!admission.governed()) return null;
        var scope = resources.objects(admission.actor().context(), IamAction.PLATFORM_ASSIGNMENT_READ);
        return scope.coversAll() ? null : scope.clauses().stream().filter(clause -> clause.departmentSets().isEmpty())
                .flatMap(clause -> clause.requiredIds().stream()).distinct().toList();
    }
}
