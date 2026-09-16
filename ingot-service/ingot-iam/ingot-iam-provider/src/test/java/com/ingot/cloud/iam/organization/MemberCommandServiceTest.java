package com.ingot.cloud.iam.organization;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.MemberStatusInput;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证成员 HTTP 编排使用生产 Guard：无对应 ACTION 时不写状态，有授权才改当前租户成员。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class MemberCommandServiceTest {
    private JdbcTemplate jdbc;
    private MemberCommandService service;
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16), version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, owner_member_id BIGINT, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT, display_name VARCHAR(128), status VARCHAR(16), version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT, is_primary BOOLEAN, PRIMARY KEY(tenant_id,member_id,department_id))");
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),actor_account_id BIGINT,actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,target_type VARCHAR(64),target_id VARCHAR(128),change_type VARCHAR(64),safe_before VARCHAR(4096),safe_after VARCHAR(4096),revisions VARCHAR(4096),delegation_id BIGINT,assignment_id BIGINT,trace_id VARCHAR(128),occurred_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_action(id BIGINT PRIMARY KEY, code VARCHAR(192), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_grant(revision_id BIGINT, action_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, tenant_member_id BIGINT, revision_id BIGINT,
                  status VARCHAR(16), valid_until TIMESTAMP, delegation_grant_id BIGINT)
                """);
        jdbc.update("INSERT INTO iam_account VALUES (1,TRUE,NULL,0),(2,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_platform_member VALUES (1001,1,'ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,101,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'管理者','ACTIVE',0,NULL),(102,2,10,'成员','ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_department VALUES (11,10,'研发')");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,102,11,TRUE)");
        jdbc.update("INSERT INTO iam_action VALUES (1,?,TRUE)", IamAction.VALUE_TENANT_MEMBER_STATUS);
        jdbc.update("INSERT INTO iam_role_grant VALUES (12,1)");
        jdbc.update("INSERT INTO iam_role_assignment VALUES (22,'TENANT',10,'MEMBER',NULL,101,12,'ACTIVE',NULL,NULL)");
        IamActionAuthorizer authorizer = (actor, action) -> {
            if (action != IamAction.TENANT_MEMBER_STATUS) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            Integer grants = jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_grant", Integer.class);
            if (grants == null || grants < 1) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            return new IamActionAuthorizer.Admission(true);
        };
        var identities = new ActiveIdentityService(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.identity(dataSource));
        var members = com.ingot.cloud.iam.persistence.IamMybatisTestAccess.members(dataSource);
        service = new MemberCommandService(new CurrentIdentityService(identities),
                new MemberLifecycle(new DataSourceTransactionManager(dataSource), identities, members,
                        com.ingot.cloud.iam.persistence.IamMybatisTestAccess.audits(dataSource),
                        new AuthorizationChangeNotifier(event -> { })),
                new GrantPresenceMemberGuard(authorizer),
                new AtomicLong(9000)::incrementAndGet, members);
        authenticate();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingActionLeavesMemberUnchanged() {
        jdbc.update("DELETE FROM iam_role_grant");
        BizException failure = assertThrows(BizException.class,
                () -> service.changeStatus(AuthorizationDomain.TENANT, "102",
                        new MemberStatusInput(MemberStatus.SUSPENDED, "0")));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_tenant_member WHERE id=102", String.class));
    }

    @Test
    void grantedActionSuspendsOnlyCurrentTenantMember() {
        var result = service.changeStatus(AuthorizationDomain.TENANT, "102",
                new MemberStatusInput(MemberStatus.SUSPENDED, "0"));
        assertEquals("102", result.id());
        assertEquals("1", result.version());
        assertEquals("SUSPENDED", jdbc.queryForObject("SELECT status FROM iam_tenant_member WHERE id=102", String.class));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_tenant_member WHERE id=101", String.class));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_platform_member WHERE id=1001", String.class));
    }

    @Test
    void platformPathRejectsTenantSession() {
        BizException failure = assertThrows(BizException.class,
                () -> service.changeStatus(AuthorizationDomain.PLATFORM, "1001",
                        new MemberStatusInput(MemberStatus.SUSPENDED, "0")));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
    }

    private void authenticate() {
        var user = InUser.stateless(1L, 10L, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder().authorizationContext(TENANT).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
