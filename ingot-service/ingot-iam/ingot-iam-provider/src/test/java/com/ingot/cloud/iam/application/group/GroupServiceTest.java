package com.ingot.cloud.iam.application.group;

import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.infrastructure.persistence.OrganizationMemberRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import java.math.BigInteger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.application.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.application.identity.ActiveIdentityService;
import com.ingot.cloud.iam.application.identity.CurrentIdentityService;
import com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.cloud.iam.application.group.model.GroupDraft;
import com.ingot.cloud.iam.application.group.model.GroupUpdateInput;
import com.ingot.framework.commons.model.iam.IamAction;

import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证扩大用户组不能让委派派生授权落到接收人以外的成员，展开口径与求值一致。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class GroupServiceTest {
    private JdbcTemplate jdbc;
    private GroupService service;
    private ResourceScopeAccessService scopes;
    private static final AuthorizationContext TENANT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");

    /** 整个类共用一个库与一份 MyBatis 配置，避免逐用例重建导致内存堆积。 */
    private static DataSource dataSource;

    @BeforeAll
    static void source() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void database() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY, enabled BOOLEAN, deleted_at TIMESTAMP,"
                + " version BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16),"
                + " version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, owner_member_id BIGINT, enabled BOOLEAN,"
                + " deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT,"
                + " display_name VARCHAR(128), status VARCHAR(16), version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(128),"
                + " description VARCHAR(256), version BIGINT DEFAULT 0, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT, group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT, group_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT,"
                + " name VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT,"
                + " is_primary BOOLEAN)");
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, platform_group_id BIGINT,
                  tenant_member_id BIGINT, tenant_group_id BIGINT, revision_id BIGINT, revision_kind VARCHAR(24),
                  scope_bindings VARCHAR(1024), delegation_grant_id BIGINT, valid_from TIMESTAMP,
                  valid_until TIMESTAMP, status VARCHAR(16), source VARCHAR(16), version BIGINT DEFAULT 0,
                  created_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT, domain VARCHAR(16),"
                + " tenant_id BIGINT, platform_member_id BIGINT, tenant_member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_department(delegation_id BIGINT, tenant_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),"
                + "actor_account_id BIGINT,actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,"
                + "target_type VARCHAR(64),target_id VARCHAR(128),change_type VARCHAR(64),safe_before VARCHAR(4096),"
                + "safe_after VARCHAR(4096),revisions VARCHAR(4096),delegation_id BIGINT,assignment_id BIGINT,"
                + "trace_id VARCHAR(128),occurred_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_account VALUES (1,TRUE,NULL,0),(2,TRUE,NULL,0),(3,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,101,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'管理员','ACTIVE',0,NULL),"
                + "(102,2,10,'总部成员','ACTIVE',0,NULL),(103,3,10,'研发成员','ACTIVE',0,NULL)");
        // 部门树 700 → 710；成员 102 在 700，成员 103 在下级 710。
        jdbc.update("INSERT INTO iam_department VALUES (700,10,NULL,'总部'),(710,10,700,'研发')");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,102,700,TRUE),(10,103,710,TRUE)");
        jdbc.update("INSERT INTO iam_tenant_group VALUES (502,10,'客服组','',0,NULL,NULL),"
                + "(503,10,'研发组','',0,NULL,NULL)");
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,502,700,FALSE)");
        // 组 502 上挂着一条来自委派 61 的派生授权，委派只接收部门 700 本级。
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_group_id,revision_id,"
                + "revision_kind,scope_bindings,delegation_grant_id,status,source,version)"
                + " VALUES (81,'TENANT',10,'GROUP',502,32,'TENANT_CUSTOM','{}',61,'ACTIVE','MANUAL',0)");
        jdbc.update("INSERT INTO iam_delegation_recipient_department VALUES (61,10,700,FALSE)");
        var identities = new ActiveIdentityService(IamMybatisTestAccess.identity(dataSource));
        var transactions = new DataSourceTransactionManager(dataSource);
        IamActionAuthorizer authorizer = (actor, action) -> new IamActionAuthorizer.Admission(true);
        var access = new IamAccess(new CurrentIdentityService(identities), authorizer,
                new AtomicLong(9000)::incrementAndGet, org.mockito.Mockito.mock(com.ingot.cloud.iam.application.authorization.PlatformAdministratorGuard.class));
        scopes = mock(ResourceScopeAccessService.class);
        var all = ObjectScope.all();
        when(scopes.objects(any(), any())).thenReturn(all);
        when(scopes.objectsForExecution(any(), any())).thenReturn(all);
        when(scopes.memberRead(any(), any())).thenReturn(all);
        when(scopes.departmentRead(any(), any())).thenReturn(all);
        when(scopes.memberReadForExecution(any(), any())).thenReturn(all);
        when(scopes.departmentReadForExecution(any(), any())).thenReturn(all);
        when(scopes.departmentVisible(any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        when(scopes.visibleMembers(any(), any(), any())).thenAnswer(invocation ->
                java.util.Set.copyOf(invocation.<java.util.Collection<BigInteger>>getArgument(2)));
        service = new GroupService(access, IamMybatisTestAccess.audits(dataSource),
                new AuthorizationChangeNotifier(event -> { }), IamMybatisTestAccess.groups(dataSource),
                IamMybatisTestAccess.recipients(dataSource),
                new DepartmentClosure(IamMybatisTestAccess.mapper(dataSource, IamDepartmentMapper.class)),
                scopes, new OrganizationMemberRepository(
                        IamMybatisTestAccess.mapper(dataSource, IamTenantMemberMapper.class)), transactions);
        authenticate();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listFiltersByName() {
        var all = service.list(AuthorizationDomain.TENANT, 1, 20, null);
        assertEquals(2, all.total());
        var hit = service.list(AuthorizationDomain.TENANT, 1, 20, "客服");
        assertEquals(1, hit.total());
        assertEquals("客服组", hit.items().getFirst().record().name());
        var miss = service.list(AuthorizationDomain.TENANT, 1, 20, "不存在");
        assertEquals(0, miss.total());
        var blank = service.list(AuthorizationDomain.TENANT, 1, 20, "  ");
        assertEquals(2, blank.total());
    }

    @Test
    void summaryAndFullPageKeepScopeWhileCountingOverlappingSourcesOnce() {
        jdbc.update("UPDATE iam_tenant_group_department SET include_descendants=TRUE WHERE group_id=502");
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (10,502,102),(10,502,103)");
        var summary = service.listSummaries(AuthorizationDomain.TENANT, 1, 1, "客服");
        assertEquals(1, summary.total());
        var item = summary.items().getFirst();
        assertEquals("502", item.record().id());

        var detail = service.list(AuthorizationDomain.TENANT, 1, 1, "客服").items().getFirst();
        assertEquals(item.capabilities(), detail.capabilities());
        assertEquals(item.version(), detail.version());
        assertEquals(List.of("102", "103"), detail.record().selection().members());
        assertEquals(2L, detail.record().visibleMemberCount());
        assertTrue(detail.record().selectionEditable());

        var limited = ObjectScope.of(List.of(new com.ingot.cloud.iam.domain.authorization.ObjectScopeClause(
                java.util.Set.of(BigInteger.valueOf(102)), List.of())));
        when(scopes.memberRead(any(), any())).thenReturn(limited);
        var visible = service.list(AuthorizationDomain.TENANT, 1, 1, "客服").items().getFirst().record();
        assertEquals(List.of("102"), visible.selection().members());
        assertEquals(1L, visible.visibleMemberCount());
        assertFalse(visible.selectionEditable());
    }

    @Test
    void scopedGroupReadAndWriteCheckTheActualTarget() {
        var selected = ObjectScope.of(List.of(new com.ingot.cloud.iam.domain.authorization.ObjectScopeClause(
                java.util.Set.of(BigInteger.valueOf(502)), List.of())));
        when(scopes.objects(TENANT, IamAction.TENANT_GROUP_READ)).thenReturn(selected);
        when(scopes.objectsForExecution(TENANT, IamAction.TENANT_GROUP_UPDATE)).thenReturn(selected);

        var page = service.list(AuthorizationDomain.TENANT, 1, 1, null);
        assertEquals(1, page.total());
        assertEquals("502", page.items().getFirst().record().id());
        var input = new GroupUpdateInput("0", new GroupDraft("仅改资料", null, null));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), assertThrows(BizException.class,
                () -> service.replace(AuthorizationDomain.TENANT, "503", input)).getCode());
        service.replace(AuthorizationDomain.TENANT, "502", input);
        assertEquals(1, count("iam_tenant_group_department"));
    }

    @Test
    void metadataEditPreservesHiddenMembersAndReplacementRejectsThem() {
        jdbc.update("INSERT INTO iam_tenant_group_member(tenant_id, group_id, member_id) VALUES (10,503,102)");
        var hidden = ObjectScope.none();
        when(scopes.memberRead(TENANT, IamAction.TENANT_MEMBER_READ)).thenReturn(hidden);
        when(scopes.memberReadForExecution(TENANT, IamAction.TENANT_MEMBER_READ)).thenReturn(hidden);
        when(scopes.visibleMembers(TENANT, hidden, List.of(BigInteger.valueOf(102)))).thenReturn(java.util.Set.of());

        var result = service.replace(AuthorizationDomain.TENANT, "503",
                new GroupUpdateInput("0", new GroupDraft("资料更新", null, null)));
        assertEquals(1, count("iam_tenant_group_member"));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), assertThrows(BizException.class,
                () -> service.replace(AuthorizationDomain.TENANT, "503",
                        new GroupUpdateInput(result.version(), draft(List.of(), List.of())))).getCode());
        assertEquals(1, count("iam_tenant_group_member"));
    }

    @Test
    void narrowingTheGroupKeepsDelegatedAssignmentsValid() {
        var replaced = service.replace(AuthorizationDomain.TENANT, "502",
                new GroupUpdateInput("0", draft(List.of("102"), List.of())));

        assertEquals(1L, replaced.record().visibleMemberCount());
        assertEquals(0, count("iam_tenant_group_department"));
    }

    @Test
    void wideningToDescendantDepartmentsBreaksTheDelegatedAssignment() {
        BizException failure = assertThrows(BizException.class, () -> service.replace(AuthorizationDomain.TENANT, "502",
                new GroupUpdateInput("0", draft(List.of(), List.of(new DepartmentSelection("700", true))))));

        assertEquals(IamReasonCode.POLICY_CONFLICT.getCode(), failure.getCode());
        assertFalse(jdbc.queryForObject("SELECT include_descendants FROM iam_tenant_group_department", Boolean.class));
    }

    @Test
    void addingAMemberOutsideTheRecipientsBreaksTheDelegatedAssignment() {
        BizException failure = assertThrows(BizException.class, () -> service.replace(AuthorizationDomain.TENANT, "502",
                new GroupUpdateInput("0", draft(List.of("103"), List.of(new DepartmentSelection("700", false))))));

        assertEquals(IamReasonCode.POLICY_CONFLICT.getCode(), failure.getCode());
        assertEquals(0, count("iam_tenant_group_member"));
    }

    @Test
    void previewExplainsTheConflictBeforeWriting() {
        var conflicting = service.preview(AuthorizationDomain.TENANT, "502",
                new GroupUpdateInput("0", draft(List.of("103"), List.of())));
        var acceptable = service.preview(AuthorizationDomain.TENANT, "502",
                new GroupUpdateInput("0", draft(List.of("102"), List.of())));

        assertFalse(conflicting.valid());
        assertEquals(IamReasonCode.POLICY_CONFLICT, conflicting.errors().getFirst().code());
        assertTrue(acceptable.valid());
        assertEquals(1, count("iam_tenant_group_department"));
    }

    @Test
    void emptyGroupsCannotCarryDelegatedAssignments() {
        BizException failure = assertThrows(BizException.class, () -> service.replace(AuthorizationDomain.TENANT, "502",
                new GroupUpdateInput("0", draft(List.of(), List.of()))));

        assertEquals(IamReasonCode.POLICY_CONFLICT.getCode(), failure.getCode());
    }

    @Test
    void groupsWithoutDelegatedAssignmentsAcceptAnySelection() {
        jdbc.update("DELETE FROM iam_role_assignment WHERE id=81");

        var replaced = service.replace(AuthorizationDomain.TENANT, "502",
                new GroupUpdateInput("0", draft(List.of("103"), List.of(new DepartmentSelection("700", true)))));

        assertEquals(2L, replaced.record().visibleMemberCount());
    }

    private static GroupDraft draft(List<String> members, List<DepartmentSelection> departments) {
        return new GroupDraft("客服组", "", new Selection(members, departments));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void authenticate() {
        var user = InUser.stateless(1L, 10L, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder().authorizationContext(TENANT).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
