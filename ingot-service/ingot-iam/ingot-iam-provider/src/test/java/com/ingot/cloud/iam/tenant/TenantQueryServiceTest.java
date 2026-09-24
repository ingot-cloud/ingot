package com.ingot.cloud.iam.tenant;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.OwnerTransferInput;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证所有者转交同步初始化系统治理授权，并保留旧所有者独立授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantQueryServiceTest {
    private JdbcTemplate jdbc;
    private TenantQueryService service;
    private final AtomicInteger invalidations = new AtomicInteger();
    private static final AuthorizationContext TENANT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final AuthorizationContext PLATFORM =
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");

    @BeforeEach
    void database() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY, enabled BOOLEAN, deleted_at TIMESTAMP,"
                + " version BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16),"
                + " version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, name VARCHAR(128), avatar VARCHAR(256),"
                + " owner_member_id BIGINT, plan_id BIGINT, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT,"
                + " created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT,"
                + " display_name VARCHAR(128), phone VARCHAR(32), email VARCHAR(64), status VARCHAR(16),"
                + " version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, platform_group_id BIGINT,
                  tenant_member_id BIGINT, tenant_group_id BIGINT, revision_id BIGINT, revision_kind VARCHAR(24),
                  scope_bindings VARCHAR(1024), delegation_grant_id BIGINT, valid_from TIMESTAMP,
                  valid_until TIMESTAMP, status VARCHAR(16), source VARCHAR(16), version BIGINT DEFAULT 0,
                  created_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),"
                + "actor_account_id BIGINT,actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,"
                + "target_type VARCHAR(64),target_id VARCHAR(128),change_type VARCHAR(64),safe_before VARCHAR(4096),"
                + "safe_after VARCHAR(4096),revisions VARCHAR(4096),delegation_id BIGINT,assignment_id BIGINT,"
                + "trace_id VARCHAR(128),occurred_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_account VALUES (1,TRUE,NULL,0),(2,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_platform_member VALUES (1001,1,'ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,'组织',NULL,101,NULL,TRUE,NULL,0,"
                + "TIMESTAMP '2026-01-02 03:04:05',NULL)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'所有者','13800000000','owner@example.com',"
                + "'ACTIVE',0,NULL),(102,2,10,'成员',NULL,NULL,'ACTIVE',0,NULL)");
        assignment(50, 101, 12, "SYSTEM", "INITIALIZATION");
        assignment(51, 101, 32, "TENANT_CUSTOM", "MANUAL");
        invalidations.set(0);
        var identities = new ActiveIdentityService(IamMybatisTestAccess.identity(dataSource));
        IamActionAuthorizer authorizer = (actor, action) -> new IamActionAuthorizer.Admission(true);
        var access = new IamAccess(new CurrentIdentityService(identities), authorizer,
                new AtomicLong(9000)::incrementAndGet);
        service = new TenantQueryService(access, IamMybatisTestAccess.audits(dataSource),
                new AuthorizationChangeNotifier(event -> invalidations.incrementAndGet()),
                IamMybatisTestAccess.tenants(dataSource), IamMybatisTestAccess.assignments(dataSource),
                new DataSourceTransactionManager(dataSource));
        authenticate(TENANT);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferMovesInitializationGovernanceAndKeepsIndependentGrant() {
        var result = service.transferOwner(new OwnerTransferInput("0", "102"));

        assertEquals("10", result.id());
        assertEquals("1", result.version());
        assertEquals(102L, jdbc.queryForObject("SELECT owner_member_id FROM iam_tenant WHERE id=10", Long.class));
        assertEquals("REVOKED", status(50));
        assertEquals("ACTIVE", status(51));
        assertEquals(101L, jdbc.queryForObject("SELECT tenant_member_id FROM iam_role_assignment WHERE id=51",
                Long.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE tenant_member_id=102"
                + " AND status='ACTIVE' AND source='INITIALIZATION' AND revision_id=12", Integer.class));
        assertEquals(1, invalidations.get());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_authorization_audit WHERE change_type="
                + "'OWNER_TRANSFER'", Integer.class));
    }

    @Test
    void transferReusesExistingAssignmentOnNewOwner() {
        assignment(52, 102, 12, "SYSTEM", "MANUAL");

        service.transferOwner(new OwnerTransferInput("0", "102"));

        assertEquals("REVOKED", status(50));
        assertEquals("ACTIVE", status(52));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE tenant_member_id=102"
                + " AND revision_id=12 AND status='ACTIVE'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE tenant_member_id=102"
                + " AND source='INITIALIZATION' AND status='ACTIVE'", Integer.class));
    }

    @Test
    void transferRejectsInactiveNewOwnerAndStaleVersion() {
        jdbc.update("UPDATE iam_tenant_member SET status='SUSPENDED' WHERE id=102");
        BizException missing = assertThrows(BizException.class,
                () -> service.transferOwner(new OwnerTransferInput("0", "102")));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), missing.getCode());
        jdbc.update("UPDATE iam_tenant_member SET status='ACTIVE' WHERE id=102");

        BizException stale = assertThrows(BizException.class,
                () -> service.transferOwner(new OwnerTransferInput("7", "102")));
        assertEquals(IamReasonCode.REVISION_CONFLICT.getCode(), stale.getCode());
        assertEquals(101L, jdbc.queryForObject("SELECT owner_member_id FROM iam_tenant WHERE id=10", Long.class));
        assertEquals("ACTIVE", status(50));
        assertEquals(0, invalidations.get());
    }

    @Test
    void transferFailsClosedWhenGovernanceAssignmentIsMissing() {
        jdbc.update("DELETE FROM iam_role_assignment WHERE id=50");
        BizException failure = assertThrows(BizException.class,
                () -> service.transferOwner(new OwnerTransferInput("0", "102")));
        assertEquals(IamReasonCode.POLICY_CONFLICT.getCode(), failure.getCode());
        assertEquals(101L, jdbc.queryForObject("SELECT owner_member_id FROM iam_tenant WHERE id=10", Long.class));
    }

    @Test
    void transferringToCurrentOwnerIsNoOp() {
        var result = service.transferOwner(new OwnerTransferInput("0", "101"));
        assertEquals("0", result.version());
        assertEquals("ACTIVE", status(50));
        assertEquals(0, invalidations.get());
    }

    @Test
    void listAndGetIncludeOwnerDisplayName() {
        assertEquals("所有者", service.settings().record().ownerDisplayName());
        assertEquals("13800000000", service.settings().record().ownerPhone());
        assertEquals("owner@example.com", service.settings().record().ownerEmail());

        authenticate(PLATFORM);
        var page = service.list(1, 20, null, null);
        assertEquals(1, page.items().size());
        assertEquals("101", page.items().getFirst().record().ownerMemberId());
        assertEquals("所有者", page.items().getFirst().record().ownerDisplayName());
        assertEquals("13800000000", page.items().getFirst().record().ownerPhone());
        assertEquals("owner@example.com", page.items().getFirst().record().ownerEmail());
        assertEquals("2026-01-02T03:04:05Z", page.items().getFirst().record().createdAt().toString());

        var detail = service.get("10");
        assertEquals("101", detail.record().ownerMemberId());
        assertEquals("所有者", detail.record().ownerDisplayName());
        assertEquals("13800000000", detail.record().ownerPhone());
        assertEquals("owner@example.com", detail.record().ownerEmail());
        assertEquals("2026-01-02T03:04:05Z", detail.record().createdAt().toString());
    }

    @Test
    void listFiltersByNameAndStatus() {
        authenticate(PLATFORM);
        jdbc.update("INSERT INTO iam_tenant VALUES (11,'研发中心',NULL,101,NULL,TRUE,NULL,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO iam_tenant VALUES (12,'测试停用',NULL,101,NULL,FALSE,NULL,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO iam_tenant VALUES (13,'已删除',NULL,101,NULL,TRUE,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");

        var byName = service.list(1, 20, "研发", null);
        assertEquals(1, byName.items().size());
        assertEquals("11", byName.items().getFirst().record().id());
        assertEquals("研发中心", byName.items().getFirst().record().name());

        var disabled = service.list(1, 20, null, "DISABLED");
        assertEquals(1, disabled.items().size());
        assertEquals("12", disabled.items().getFirst().record().id());

        var combined = service.list(1, 20, "组织", "ENABLED");
        assertEquals(1, combined.items().size());
        assertEquals("10", combined.items().getFirst().record().id());

        var none = service.list(1, 20, "不存在", null);
        assertEquals(0, none.items().size());
        assertEquals(0, none.total());

        BizException invalid = assertThrows(BizException.class, () -> service.list(1, 20, null, "ENABLE"));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), invalid.getCode());
    }

    private void assignment(long id, long memberId, long revisionId, String kind, String source) {
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,revision_id,"
                        + "revision_kind,scope_bindings,valid_from,status,source,version) VALUES "
                        + "(?,'TENANT',10,'MEMBER',?,?,?, '{}', TIMESTAMP '2000-01-01 00:00:00','ACTIVE',?,0)",
                id, memberId, revisionId, kind, source);
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT status FROM iam_role_assignment WHERE id=?", String.class, id);
    }

    private void authenticate(AuthorizationContext context) {
        Long tenantId = context.tenantId() == null ? null : Long.parseLong(context.tenantId());
        var user = InUser.stateless(1L, tenantId, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder().authorizationContext(context).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
