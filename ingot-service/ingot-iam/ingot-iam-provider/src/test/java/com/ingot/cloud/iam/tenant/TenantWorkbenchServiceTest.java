package com.ingot.cloud.iam.tenant;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.GroupRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.session.SessionService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ApplicationSummary;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Bootstrap;
import com.ingot.framework.commons.model.iam.CurrentProfile;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.MenuKind;
import com.ingot.framework.commons.model.iam.MenuNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>验证工作台指标沿用资源范围、无权限不查库，且不可跨域或把依赖失败伪装为零。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantWorkbenchServiceTest {
    private static final AuthorizationContext ACTOR =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private final SessionService sessions = mock(SessionService.class);
    private final ResourceAccess scopes = mock(ResourceAccess.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final MemberQueryRepository members = mock(MemberQueryRepository.class);
    private final DepartmentQueryRepository departments = mock(DepartmentQueryRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private TenantWorkbenchService service;

    @BeforeEach
    void setup() {
        service = new TenantWorkbenchService(sessions, scopes, tenants, members, departments, groups);
    }

    @Test
    void statisticsUsesTheSameReadScopesAsResourceLists() {
        when(sessions.bootstrap()).thenReturn(bootstrap(ACTOR, List.of(IamAction.VALUE_TENANT_MEMBER_READ,
                IamAction.VALUE_TENANT_DEPARTMENT_READ, IamAction.VALUE_TENANT_GROUP_READ), List.of()));
        tenant();
        var memberScope = ObjectScope.none();
        var departmentScope = ObjectScope.all();
        when(scopes.memberRead(ACTOR, IamAction.TENANT_MEMBER_READ)).thenReturn(memberScope);
        when(scopes.departmentRead(ACTOR, IamAction.TENANT_DEPARTMENT_READ)).thenReturn(departmentScope);
        when(members.countTenant(10, memberScope)).thenReturn(0L);
        when(departments.countDepartments(10, departmentScope)).thenReturn(3L);
        when(groups.countTenant(10)).thenReturn(2L);

        var result = service.statistics();

        assertEquals("10", result.tenantId());
        assertEquals("当前组织", result.tenantName());
        assertEquals("logos/org.png", result.tenantAvatar());
        assertEquals(0L, result.memberCount());
        assertEquals(3L, result.departmentCount());
        assertEquals(2L, result.groupCount());
        verify(members).countTenant(10, memberScope);
        verify(departments).countDepartments(10, departmentScope);
    }

    @Test
    void omittedPermissionsSkipQueriesAndOnlyNavigableApplicationsAreCounted() {
        var hiddenDirectory = new MenuNode("directory", "b", "隐藏目录", MenuKind.DIRECTORY, null,
                null, null, null, 0, List.of(menu("hidden-child", "b", false)), true, false, false);
        when(sessions.bootstrap()).thenReturn(bootstrap(ACTOR, List.of(), List.of(
                menu("one", "a", false), menu("two", "a", false), menu("hidden", "b", true),
                hiddenDirectory, menu("unavailable-app", "not-opened", false))));
        tenant();

        var result = service.statistics();

        assertNull(result.memberCount());
        assertNull(result.departmentCount());
        assertNull(result.groupCount());
        assertEquals(1L, result.applicationCount());
        verifyNoInteractions(members, departments, groups, scopes);
    }

    @Test
    void platformIdentityIsRejectedBeforeTenantQueries() {
        var platform = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "101");
        when(sessions.bootstrap()).thenReturn(bootstrap(platform, List.of(), List.of()));
        assertThrows(BizException.class, service::statistics);
        verifyNoInteractions(tenants, members, departments, groups, scopes);
    }

    @Test
    void authorizationFailurePropagatesInsteadOfReturningEmptyStatistics() {
        when(sessions.bootstrap()).thenThrow(new IllegalStateException("authorization unavailable"));
        assertThrows(IllegalStateException.class, service::statistics);
        verifyNoInteractions(tenants, members, departments, groups);
    }

    @Test
    void countFailureDoesNotReturnZero() {
        when(sessions.bootstrap()).thenReturn(bootstrap(ACTOR, List.of(IamAction.VALUE_TENANT_GROUP_READ), List.of()));
        tenant();
        when(groups.countTenant(10)).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, service::statistics);
    }

    private void tenant() {
        var tenant = new IamTenantEntity();
        tenant.setId(BigInteger.TEN);
        tenant.setName("当前组织");
        tenant.setAvatar("logos/org.png");
        when(tenants.findActive(10)).thenReturn(tenant);
    }

    private static Bootstrap bootstrap(AuthorizationContext actor, List<String> actions, List<MenuNode> menus) {
        return new Bootstrap(actor, new CurrentProfile("101", "当前成员", null),
                List.of(new ApplicationSummary("a", "app-a", "应用 A", null, 0),
                        new ApplicationSummary("b", "app-b", "应用 B", null, 1)),
                menus, actions, "1", Instant.parse("2030-01-01T00:00:00Z"));
    }

    private static MenuNode menu(String id, String applicationId, boolean hidden) {
        return new MenuNode(id, applicationId, id, MenuKind.PAGE, "/" + id, null, null, null,
                0, List.of(), hidden, false, false);
    }
}
