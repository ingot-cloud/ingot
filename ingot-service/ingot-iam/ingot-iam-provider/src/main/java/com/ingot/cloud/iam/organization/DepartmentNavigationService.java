package com.ingot.cloud.iam.organization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按既有部门读取范围提供组织人员选择导航，部门不能成为人员选择结果。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DepartmentNavigationService {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final DepartmentQueryRepository departments;

    /** 不披露不可见祖先，根入口列出允许导航的部门片段。 */
    public DepartmentNavigationPage page(String parentId, int page, int size) {
        IamPages.require(page, size);
        var actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
        long tenant = IamIds.require(actor.context().tenantId());
        var scope = scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ);
        Long parent = parentId == null || parentId.isBlank() ? null : IamIds.require(parentId);
        if (parent != null) scopes.requireVisibleDepartment(actor.context(), IamAction.TENANT_DEPARTMENT_READ, parent);
        var result = departments.navigation(tenant, scope, parent, page, size);
        var path = new ArrayList<DepartmentNavigationEntry>();
        var visited = new HashSet<Long>();
        Long cursor = parent;
        while (cursor != null && visited.add(cursor) && ObjectScopeSql.matches(scope, cursor)) {
            var row = departments.find(tenant, cursor);
            if (row == null || row.getParentId() == null) break;
            path.add(new DepartmentNavigationEntry(row.getId().toString(), row.getName()));
            cursor = row.getParentId().longValueExact();
        }
        Collections.reverse(path);
        var root = departments.navigationRoot(tenant, scope);
        return new DepartmentNavigationPage(result.getRecords().stream().map(row ->
                new DepartmentNavigationEntry(row.getId().toString(), row.getName())).toList(),
                result.getTotal(), page, size, path, root == null ? null : new DepartmentNavigationEntry(root.getId().toString(), root.getName()));
    }
}
