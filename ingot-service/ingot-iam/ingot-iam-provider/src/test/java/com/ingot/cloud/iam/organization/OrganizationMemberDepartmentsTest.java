package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.cloud.iam.evaluation.*;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证选择器任职快照同时服从成员及部门读取边界，且按页批量加载。</p>
 * @author jy
 * @since 1.0.0
 */
class OrganizationMemberDepartmentsTest {
    private final IamAccess access = mock(IamAccess.class);
    private final ResourceAccess scopes = mock(ResourceAccess.class);
    private final MemberQueryRepository members = mock(MemberQueryRepository.class);
    private final DepartmentQueryRepository departments = mock(DepartmentQueryRepository.class);
    private final OrganizationMemberDepartments service = new OrganizationMemberDepartments(access, scopes, members, departments);
    private final ActiveIdentity actor = new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.TENANT,"10","1","101"),"0","0","0");

    @Test
    void noMemberReadDoesNotEnrichDelegatedCandidates() {
        assertTrue(service.visible(actor,List.of(id(101))).isEmpty());
        verifyNoInteractions(scopes,members,departments);
    }

    @Test
    void noDepartmentReadDoesNotQueryDepartmentNames() {
        when(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_MEMBER_READ,false)).thenReturn(true);
        assertTrue(service.visible(actor,List.of(id(101))).isEmpty());
        verifyNoInteractions(scopes,members,departments);
    }

    @Test
    void currentPageIntersectsMemberAndDepartmentScopesBeforeReturningNames() {
        when(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_MEMBER_READ,false)).thenReturn(true);
        when(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_READ,false)).thenReturn(true);
        var memberScope=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(101)),List.of())));
        when(scopes.memberRead(actor.context(),IamAction.TENANT_MEMBER_READ)).thenReturn(memberScope);
        var departmentScope=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(11)),List.of())));
        when(scopes.departmentRead(actor.context(),IamAction.TENANT_DEPARTMENT_READ)).thenReturn(departmentScope);
        when(members.visibleTenantIds(10,List.of(id(101),id(102)),memberScope,true)).thenReturn(Set.of(id(101)));
        when(members.departmentViews(10,List.of(id(101)))).thenReturn(Map.of(id(101),List.of(new MemberDepartmentView("11","研发",true),new MemberDepartmentView("12","隐藏部门",false))));
        when(departments.matchingIds(10,List.of(id(11),id(12)),departmentScope)).thenReturn(Set.of(id(11)));
        assertEquals(Map.of(id(101),List.of(new MemberDepartmentView("11","研发",true))),service.visible(actor,List.of(id(101),id(102))));
        verify(members,times(1)).departmentViews(10,List.of(id(101)));
    }
    private static BigInteger id(long value) {return BigInteger.valueOf(value);}
}
