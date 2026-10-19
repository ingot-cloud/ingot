package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>为当前页可读成员批量提供可见任职，不扩大业务候选或披露隐藏部门。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class OrganizationMemberDepartments {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final MemberQueryRepository members;
    private final DepartmentQueryRepository departments;

    /** 同时具备成员和部门读取资格才附带任职，缺少资格时返回空投影。 */
    public Map<BigInteger, List<MemberDepartmentView>> visible(ActiveIdentity actor, List<BigInteger> ids) {
        if (ids.isEmpty() || !access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)
                || !access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ, false)) return Map.of();
        var scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        long tenant = IamIds.require(actor.context().tenantId());
        var allowedMembers = members.visibleTenantIds(tenant, ids, scope, true);
        var readable = ids.stream().distinct().filter(allowedMembers::contains).toList();
        if (readable.isEmpty()) return Map.of();
        var views = members.departmentViews(tenant, readable);
        var allowed = departments.matchingIds(tenant, views.values().stream().flatMap(List::stream)
                .map(value -> BigInteger.valueOf(IamIds.require(value.id()))).distinct().toList(),
                scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ));
        Map<BigInteger, List<MemberDepartmentView>> result = new LinkedHashMap<>();
        views.forEach((id, values) -> result.put(id, values.stream().filter(value -> allowed.contains(
                BigInteger.valueOf(IamIds.require(value.id())))).toList()));
        return result;
    }
}
