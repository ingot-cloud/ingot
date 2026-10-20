package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>按成员范围和浏览部门在 SQL 分页前过滤有效组织人员。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class OrganizationMemberRepository {
    private final IamTenantMemberMapper members;

    /** 已知ID仅作少量回显，使用相同租户、成员范围和账号可用边界。 */
    public Page<IamTenantMemberEntity> page(long tenantId, ObjectScope scope, Long departmentId, String keyword,
                                          List<BigInteger> selected, int page, int size) {
        return page(tenantId, scope, departmentId, keyword, selected, page, size, null);
    }

    /** 按明确组关联分页回显，避免把完整关系ID拼回成员列表。 */
    public Page<IamTenantMemberEntity> page(long tenantId, ObjectScope scope, Long departmentId, String keyword,
                                          List<BigInteger> selected, int page, int size, Long groupId) {
        return page(tenantId, scope, departmentId, keyword, selected, page, size, groupId, null);
    }

    /** 选择器分支与现有成员范围相交，根未分配判断覆盖本租户全部实际关系。 */
    public Page<IamTenantMemberEntity> page(long tenantId, ObjectScope scope, Long departmentId, String keyword,
                                          List<BigInteger> selected, int page, int size, Long groupId, MemberPickerBranch branch) {
        var query = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .select(IamTenantMemberEntity::getId, IamTenantMemberEntity::getDisplayName,
                        IamTenantMemberEntity::getAvatar, IamTenantMemberEntity::getStatus)
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .inSql(IamTenantMemberEntity::getAccountId, "SELECT id FROM iam_account WHERE enabled=TRUE AND deleted_at IS NULL");
        ObjectScopeSql.restrictTenantMembers(query, scope, BigInteger.valueOf(tenantId));
        if (selected.isEmpty() && groupId == null) query.eq(IamTenantMemberEntity::getStatus, MemberStatus.ACTIVE);
        if (!selected.isEmpty()) query.in(IamTenantMemberEntity::getId, selected);
        if (groupId != null) query.apply("EXISTS(SELECT 1 FROM iam_tenant_group_member gm WHERE gm.tenant_id={0} AND gm.member_id=iam_tenant_member.id AND gm.group_id={1})", tenantId, groupId);
        if (keyword != null && !keyword.isBlank()) {
            String search = "%" + keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            query.apply("display_name LIKE {0} ESCAPE '!'", search);
        } else if (departmentId != null) query.apply("EXISTS(SELECT 1 FROM iam_member_department md WHERE md.tenant_id={0} AND md.member_id=iam_tenant_member.id AND md.department_id={1})", tenantId, departmentId);
        if (branch != null) {
            String predicate = branch.sql("iam_tenant_member.id", "iam_tenant_member.tenant_id", "{0}");
            if (branch.departmentId() == null) query.apply(predicate);
            else query.apply(predicate, branch.departmentId());
        }
        return members.selectPage(new Page<>(page, size), query.orderByAsc(IamTenantMemberEntity::getId));
    }
    /** 批量判断真实零任职，不使用过滤后的可见部门投影推断。 */
    public java.util.Set<BigInteger> unassigned(long tenantId, List<BigInteger> ids) {
        return unassigned(tenantId, ids, ObjectScope.all());
    }

    /** 零关系显示投影仍与成员完整SQL范围求交，保留ID与部门集合的交集。 */
    public java.util.Set<BigInteger> unassigned(long tenantId, List<BigInteger> ids, ObjectScope scope) {
        if (ids.isEmpty()) return java.util.Set.of();
        var query = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .select(IamTenantMemberEntity::getId).eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .in(IamTenantMemberEntity::getId, ids)
                .apply("NOT EXISTS(SELECT 1 FROM iam_member_department md WHERE md.tenant_id=iam_tenant_member.tenant_id AND md.member_id=iam_tenant_member.id)");
        ObjectScopeSql.restrictTenantMembers(query, scope, BigInteger.valueOf(tenantId));
        return members.selectList(query).stream().map(IamTenantMemberEntity::getId).collect(java.util.stream.Collectors.toSet());
    }
}
