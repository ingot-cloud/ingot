package com.ingot.cloud.iam.application.account;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;

import com.ingot.cloud.iam.application.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.application.authorization.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.domain.authorization.ObjectScopeClause;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.FieldMergeMode;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import com.ingot.cloud.iam.application.identity.ActiveIdentityService;
import com.ingot.cloud.iam.application.identity.CurrentIdentityService;
import com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.application.account.model.AccountLookupInput;
import com.ingot.cloud.iam.domain.account.AccountLookupPurpose;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.shared.VersionInput;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.port.inbound.ChangePasswordUseCase;
import com.ingot.framework.security.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.security.account.domain.port.inbound.ManageAccountStatusUseCase;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.security.account.domain.port.inbound.UnlockAccountUseCase;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证平台账号查询受精确 ACTION 与对象范围约束，查找不返回组织关系。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class PlatformAccountServiceTest {
    private static final AuthorizationContext PLATFORM =
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    private static DataSource dataSource;
    private JdbcTemplate jdbc;
    private PlatformAccountService service;
    private IamActionAuthorizer authorizer;
    private ResourceScopeAccessService scopes;
    private RoleFieldPermissionService memberFields;

    /** 整个类共用一个库与一份 MyBatis 配置，避免逐用例重建导致内存堆积。 */
    @BeforeAll
    static void source() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void database() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE iam_account(id BIGINT PRIMARY KEY, username VARCHAR(64), password_hash VARCHAR(300),
                  phone VARCHAR(32), email VARCHAR(128), enabled BOOLEAN, must_change_password BOOLEAN,
                  password_changed_at TIMESTAMP, last_login_at TIMESTAMP, version BIGINT, created_at TIMESTAMP,
                  updated_at TIMESTAMP, deleted_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, display_name VARCHAR(128), avatar VARCHAR(256), phone VARCHAR(32), email VARCHAR(128), status VARCHAR(16), version BIGINT, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, name VARCHAR(128), enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT, display_name VARCHAR(128), status VARCHAR(16), version BIGINT)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT, is_primary BOOLEAN)");
        jdbc.execute("""
                CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),actor_account_id BIGINT,
                  actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,target_type VARCHAR(64),target_id VARCHAR(128),
                  change_type VARCHAR(64),safe_before VARCHAR(4096),safe_after VARCHAR(4096),revisions VARCHAR(4096),
                  delegation_id BIGINT,assignment_id BIGINT,trace_id VARCHAR(128),occurred_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE account_lock_state(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, user_type VARCHAR(16),
                  locked BOOLEAN, lock_type VARCHAR(16), lock_reason_code VARCHAR(64), lock_reason_detail VARCHAR(255),
                  locked_at TIMESTAMP, locked_until TIMESTAMP, operator_id BIGINT, operator_name VARCHAR(64),
                  failed_login_count INT, last_failed_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO iam_account(id,username,password_hash,phone,email,enabled,must_change_password,version)
                VALUES (1,'alice','hash','13800000000','alice@example.com',TRUE,FALSE,0),
                       (2,'bob','hash',NULL,NULL,TRUE,FALSE,0)
                """);
        jdbc.update("INSERT INTO iam_platform_member(id,account_id,display_name,avatar,status,version,created_at,updated_at) VALUES (1001,1,'管理者',NULL,'ACTIVE',0,NULL,NULL)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,'组织A',TRUE,NULL,1)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,2,10,'成员','ACTIVE',0)");
        var identities = new ActiveIdentityService(IamMybatisTestAccess.identity(dataSource));
        authorizer = (actor, action) -> new IamActionAuthorizer.Admission(true);
        IamAccess access = new IamAccess(new CurrentIdentityService(identities),
                (actor, action) -> authorizer.admit(actor, action), new AtomicLong(9000)::incrementAndGet, org.mockito.Mockito.mock(com.ingot.cloud.iam.application.authorization.PlatformAdministratorGuard.class));
        scopes = mock(ResourceScopeAccessService.class);
        when(scopes.objects(any(), any())).thenReturn(ObjectScope.all());
        doAnswer(invocation -> null).when(scopes).requireVisibleObject(any(), any(), anyLong());
        AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
        when(evaluator.evaluate(any())).thenReturn(new AuthorizationView(
                List.of(IamAction.VALUE_PLATFORM_ACCOUNT_UPDATE, IamAction.VALUE_PLATFORM_ACCOUNT_DELETE),
                List.of(), Map.of(), "1", Instant.now().plusSeconds(60)));
        when(scopes.targetAllowed(any(), any(), any(), any())).thenReturn(true);
        memberFields = mock(RoleFieldPermissionService.class);
        var full = new FieldAccess(FieldVisibility.FULL, true);
        var fields = Map.of("phone", full, "email", full);
        when(memberFields.previewAll(any(), any(), anyList())).thenReturn(Map.of(IamAction.PLATFORM_MEMBER_CREATE, new FieldPolicyDecision(fields, fields, List.of())));
        service = new PlatformAccountService(access, scopes, new ObjectCapabilities(evaluator, scopes),
                IamMybatisTestAccess.accountQueries(dataSource),
                IamMybatisTestAccess.accountWrites(dataSource, () -> 99L), IamMybatisTestAccess.accounts(dataSource),
                IamMybatisTestAccess.memberQueries(dataSource), IamMybatisTestAccess.audits(dataSource),
                mock(RegisterUserUseCase.class), mock(ManageAccountStatusUseCase.class),
                mock(LockAccountUseCase.class), mock(UnlockAccountUseCase.class),
                mock(ChangePasswordUseCase.class), mock(InitialPasswordService.class),
                new DataSourceTransactionManager(dataSource), memberFields, new com.ingot.framework.authorization.field.DefaultMaskStrategy());
        authenticate();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingActionLeavesAccountsUnread() {
        authorizer = (actor, action) -> {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        };
        BizException failure = assertThrows(BizException.class, () -> service.list(1, 20));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
    }

    @Test
    void lookupMissSaysAccountMissing() {
        BizException failure = assertThrows(BizException.class,
                () -> service.lookup(new AccountLookupInput(AccountLookupPurpose.ACCOUNT_MANAGE, "nobody", null, null)));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), failure.getCode());
        assertEquals("账号不存在", failure.getMessage());
    }

    @Test
    void memberCreateLookupReturnsLoginContactsWithoutOrgRelations() {
        var result = service.lookup(new AccountLookupInput(AccountLookupPurpose.MEMBER_CREATE, "alice", null, null));
        assertEquals("1", result.record().id());
        assertEquals("alice", result.record().username());
        assertEquals("13800000000", result.record().phone());
        assertEquals("alice@example.com", result.record().email());
        assertEquals("0", result.version());
    }

    @Test
    void memberCreateLookupRejectsExistingPlatformMembershipWhenDomainIsPlatform() {
        BizException failure = assertThrows(BizException.class, () -> service.lookup(
                new AccountLookupInput(AccountLookupPurpose.MEMBER_CREATE, "alice", null, null,
                        AuthorizationDomain.PLATFORM)));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), failure.getCode());
        assertEquals("该账号已是平台成员", failure.getMessage());
    }

    @Test
    void memberCreateLookupAllowsAccountWithoutPlatformMembershipWhenDomainIsPlatform() {
        var result = service.lookup(new AccountLookupInput(AccountLookupPurpose.MEMBER_CREATE, "bob", null, null,
                AuthorizationDomain.PLATFORM));
        assertEquals("2", result.record().id());
        assertEquals("bob", result.record().username());
    }

    @Test
    void platformCreateLookupMasksAndHidesContactsBeforeReturningThem() {
        var hidden = new FieldAccess(FieldVisibility.HIDDEN, false);
        var full = new FieldAccess(FieldVisibility.FULL, true);
        var masked = new FieldAccess(FieldVisibility.MASKED, false);
        var scope = List.of(new ScopeCondition(true, List.of(), null, List.of()));
        when(memberFields.previewAll(any(), any(), anyList())).thenReturn(Map.of(IamAction.PLATFORM_MEMBER_CREATE, new FieldPolicyDecision(
                Map.of("phone", hidden, "email", hidden), Map.of("phone", full, "email", full),
                List.of(new ResolvedFieldRule("phone", scope, masked), new ResolvedFieldRule("email", scope, hidden)),
                Map.of(), Map.of("phone", com.ingot.framework.commons.model.iam.MaskSpec.PHONE), FieldMergeMode.GRANTS)));
        jdbc.update("UPDATE iam_account SET phone='13900000002',email='bob@example.com' WHERE id=2");
        var result = service.lookup(new AccountLookupInput(AccountLookupPurpose.MEMBER_CREATE, "bob", null, null,
                AuthorizationDomain.PLATFORM));
        assertEquals("139****0002", result.record().phone());
        assertEquals(null, result.record().email());
        assertEquals(masked, result.fieldAccess().get("phone"));
        assertEquals(hidden, result.fieldAccess().get("email"));
    }

    @Test
    void platformCreateLookupCannotBeUsedWithOnlyAccountLookupPermission() {
        authorizer = (actor, action) -> {
            if (action == IamAction.PLATFORM_MEMBER_CREATE) throw new BizException(IamReasonCode.ACTION_DENIED);
            return new IamActionAuthorizer.Admission(true);
        };
        var failure = assertThrows(BizException.class, () -> service.lookup(new AccountLookupInput(
                AccountLookupPurpose.MEMBER_CREATE, "bob", null, null, AuthorizationDomain.PLATFORM)));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
        verify(memberFields, never()).previewAll(any(), any(), anyList());
    }

    @Test
    void objectScopeHidesUnlistedAccounts() {
        when(scopes.objects(any(), any())).thenReturn(ObjectScope.of(
                List.of(new ObjectScopeClause(Set.of(BigInteger.valueOf(2)), List.of()))));
        var page = service.list(1, 20);
        assertEquals(1, page.items().size());
        assertEquals("2", page.items().getFirst().record().id());
    }

    @Test
    void memberCandidatePageExcludesAllPlatformMembershipStatusesBeforeCountingAndPaging() {
        jdbc.update("""
                INSERT INTO iam_account(id,username,version) VALUES
                  (3,'suspended',0),(4,'removed',0),(5,'new-account',0)
                """);
        jdbc.update("""
                INSERT INTO iam_platform_member(id,account_id,status,version) VALUES
                  (1003,3,'SUSPENDED',0),(1004,4,'REMOVED',0)
                """);

        var first = service.list(1, 1, true);
        var second = service.list(2, 1, true);
        assertEquals(2, first.total());
        assertEquals(2, second.total());
        assertEquals("2", first.items().getFirst().record().id());
        assertEquals("5", second.items().getFirst().record().id());

        var unfiltered = service.list(1, 1, false);
        assertEquals(5, unfiltered.total());
        assertEquals("1", unfiltered.items().getFirst().record().id());
    }

    @Test
    void memberCandidatePageIntersectsReadAndLookupScopes() {
        jdbc.update("INSERT INTO iam_account(id,username,version) VALUES (3,'read-only',0),(4,'lookup-only',0)");
        when(scopes.objects(any(), eq(IamAction.PLATFORM_ACCOUNT_READ))).thenReturn(ObjectScope.of(
                List.of(new ObjectScopeClause(Set.of(BigInteger.TWO, BigInteger.valueOf(3)), List.of()))));
        when(scopes.objects(any(), eq(IamAction.PLATFORM_ACCOUNT_LOOKUP))).thenReturn(ObjectScope.of(
                List.of(new ObjectScopeClause(Set.of(BigInteger.TWO, BigInteger.valueOf(4)), List.of()))));

        var page = service.list(1, 20, true);
        assertEquals(1, page.total());
        assertEquals("2", page.items().getFirst().record().id());
        verify(scopes).requireCreate(PLATFORM, IamAction.PLATFORM_MEMBER_CREATE, Set.of());
    }

    @Test
    void accountReadAndLookupAloneCannotFilterByPlatformMembership() {
        authorizer = (actor, action) -> {
            if (action == IamAction.PLATFORM_MEMBER_CREATE) throw new BizException(IamReasonCode.ACTION_DENIED);
            return new IamActionAuthorizer.Admission(true);
        };
        var failure = assertThrows(BizException.class, () -> service.list(1, 20, true));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
        assertEquals(2, service.list(1, 20, false).total());
    }

    @Test
    void listReusesAuthorizationSnapshotForWriteCapabilities() {
        var page = service.list(1, 20);
        assertEquals(2, page.items().size());
        verify(scopes, times(1)).objects(any(), eq(IamAction.PLATFORM_ACCOUNT_READ));
        verify(scopes, never()).objects(any(), eq(IamAction.PLATFORM_ACCOUNT_UPDATE));
        verify(scopes, never()).objects(any(), eq(IamAction.PLATFORM_ACCOUNT_DELETE));
        verify(scopes).targetAllowed(any(), any(), eq(IamAction.VALUE_PLATFORM_ACCOUNT_UPDATE), eq("1"));
    }

    @Test
    void deleteFailsWhenAccountStillHasMembership() {
        BizException failure = assertThrows(BizException.class,
                () -> service.delete("2", new VersionInput("0")));
        assertEquals(IamReasonCode.OBJECT_IN_USE.getCode(), failure.getCode());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_account WHERE id=2 AND deleted_at IS NULL",
                Integer.class));
    }

    private void authenticate() {
        var user = InUser.stateless(1L, null, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder().authorizationContext(PLATFORM).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
