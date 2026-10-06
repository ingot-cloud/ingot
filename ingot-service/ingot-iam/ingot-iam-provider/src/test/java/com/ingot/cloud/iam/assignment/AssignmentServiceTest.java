package com.ingot.cloud.iam.assignment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.role.RoleGrantValidator;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.role.RoleSynthesisCache;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AssignmentBatchInput;
import com.ingot.framework.commons.model.iam.AssignmentInput;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.SubjectRef;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.commons.model.iam.ValidationIssue;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证治理授权与受限委派分开准入：受限方必须使用属于自己的单一委派，且不得越过委派的角色、接收人与范围上限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AssignmentServiceTest {
    private JdbcTemplate jdbc;
    private AssignmentService service;
    /** 逐例设定，模拟当前操作者的 ACTION 是否来自非委派授权。 */
    private boolean governed;
    private boolean administrator;
    private static final AuthorizationContext TENANT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final String MANAGED_DEPARTMENTS =
            "[{\"kind\":\"MANAGED_DEPARTMENTS\",\"parameterKey\":\"scope\",\"includeDescendants\":false}]";

    /** 整个类共用一个库与一份 MyBatis 配置，避免逐用例重建导致内存堆积。 */
    private static DataSource dataSource;

    @BeforeAll
    static void source() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void database() {
        administrator = false;
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE iam_application(id BIGINT PRIMARY KEY, code VARCHAR(64), domain VARCHAR(16))");
        jdbc.update("INSERT INTO iam_application VALUES (1,'iam-platform','PLATFORM')");
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY, enabled BOOLEAN, deleted_at TIMESTAMP,"
                + " version BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16),"
                + " version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, owner_member_id BIGINT, enabled BOOLEAN,"
                + " deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT,"
                + " display_name VARCHAR(128), status VARCHAR(16), version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT PRIMARY KEY, tenant_id BIGINT, code VARCHAR(64),"
                + " name VARCHAR(128), description VARCHAR(256), version BIGINT DEFAULT 0)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT,"
                + " name VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT,"
                + " is_primary BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT, group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT, group_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,"
                + " kind VARCHAR(24), code VARCHAR(64), name VARCHAR(128), description VARCHAR(256),"
                + " group_name VARCHAR(64), enabled BOOLEAN DEFAULT TRUE, version BIGINT DEFAULT 0)");
        jdbc.execute("CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY, role_id BIGINT, kind VARCHAR(24),"
                + " revision BIGINT DEFAULT 1, base_revision_id BIGINT, metadata_overrides VARCHAR(1024), resource_field_permissions VARCHAR(16384) DEFAULT '{}',"
                + " published_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_role_grant(revision_id BIGINT, action_id BIGINT, scopes VARCHAR(512))");
        jdbc.execute("CREATE TABLE iam_role_delta(revision_id BIGINT, action_id BIGINT, operation VARCHAR(32),"
                + " scopes VARCHAR(512))");
        jdbc.execute("CREATE TABLE iam_role_parameter(revision_id BIGINT, parameter_key VARCHAR(64),"
                + " binding_kind VARCHAR(24))");
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, platform_group_id BIGINT,
                  tenant_member_id BIGINT, tenant_group_id BIGINT, revision_id BIGINT, revision_kind VARCHAR(24),
                  scope_bindings VARCHAR(1024), delegation_grant_id BIGINT, valid_from TIMESTAMP,
                  valid_until TIMESTAMP, status VARCHAR(16), source VARCHAR(16), version BIGINT DEFAULT 0,
                  created_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  platform_administrator_id BIGINT, tenant_administrator_id BIGINT, valid_from TIMESTAMP,
                  valid_until TIMESTAMP, max_assignment_duration_seconds BIGINT,
                  max_assignment_duration_nanos INT, assignment_duration_mode VARCHAR(16) DEFAULT 'LIMITED', status VARCHAR(16), version BIGINT DEFAULT 0,
                  created_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_delegation_role_revision(delegation_id BIGINT, revision_id BIGINT,"
                + " revision_kind VARCHAR(24))");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT, domain VARCHAR(16),"
                + " tenant_id BIGINT, platform_member_id BIGINT, tenant_member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_department(delegation_id BIGINT, tenant_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_delegation_action_ceiling(delegation_id BIGINT, action_id BIGINT,"
                + " scopes VARCHAR(512), scope_bindings VARCHAR(1024))");
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),"
                + "actor_account_id BIGINT,actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,"
                + "target_type VARCHAR(64),target_id VARCHAR(128),change_type VARCHAR(64),safe_before VARCHAR(4096),"
                + "safe_after VARCHAR(4096),revisions VARCHAR(4096),delegation_id BIGINT,assignment_id BIGINT,"
                + "trace_id VARCHAR(128),occurred_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_account VALUES (1,TRUE,NULL,0),(2,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_platform_member VALUES (1001,1,'ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,101,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'受限管理员','ACTIVE',0,NULL),"
                + "(102,2,10,'接收成员','ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_department VALUES (700,10,NULL,'总部'),(710,10,700,'研发')");
        // 目标角色是本租户自定义角色，只授予一个带管理部门参数的操作。
        jdbc.update("INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name,enabled,version)"
                + " VALUES (22,'TENANT',10,'TENANT_CUSTOM','custom','客户管理',TRUE,0)");
        jdbc.update("INSERT INTO iam_role_revision(id,role_id,kind,revision) VALUES (32,22,'TENANT_CUSTOM',1)");
        jdbc.update("INSERT INTO iam_role_grant VALUES (32,12,?)", MANAGED_DEPARTMENTS);
        jdbc.update("INSERT INTO iam_role_parameter VALUES (32,'scope','DEPARTMENTS')");
        var identities = new ActiveIdentityService(IamMybatisTestAccess.identity(dataSource));
        var transactions = new DataSourceTransactionManager(dataSource);
        var audits = IamMybatisTestAccess.audits(dataSource);
        var changes = new AuthorizationChangeNotifier(event -> { });
        IamActionAuthorizer authorizer = new IamActionAuthorizer() {
            @Override public Admission admit(AuthorizationContext actor, IamAction action) { return new Admission(governed); }
            @Override public boolean platformAdministrator(AuthorizationContext actor) { return administrator; }
        };
        var access = new IamAccess(new CurrentIdentityService(identities), authorizer,
                new AtomicLong(9000)::incrementAndGet, org.mockito.Mockito.mock(com.ingot.cloud.iam.support.PlatformAdministratorGuard.class));
        var roles = new RoleService(access, audits, changes, new RoleSynthesisCache(),
                new RoleGrantValidator(IamMybatisTestAccess.roles(dataSource)),
                IamMybatisTestAccess.delegationAdmission(dataSource), IamMybatisTestAccess.roles(dataSource),
                IamMybatisTestAccess.catalogService(access, dataSource, transactions),
                IamMybatisTestAccess.roleFields(dataSource), IamMybatisTestAccess.fieldMetadata(dataSource), transactions);
        service = new AssignmentService(access, audits, changes, roles, IamMybatisTestAccess.roles(dataSource),
                IamMybatisTestAccess.assignments(dataSource), IamMybatisTestAccess.delegationAdmission(dataSource),
                org.mockito.Mockito.mock(com.ingot.cloud.iam.evaluation.ResourceAccess.class),
                org.mockito.Mockito.mock(PlatformAuthorizationEditor.class),
                org.mockito.Mockito.mock(TenantScopeCandidates.class), transactions);
        governed = true;
        authenticate();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void governedActorAssignsWithoutAnyDelegation() {
        service.create(AuthorizationDomain.TENANT, batch(null, "700"));

        assertEquals(1, count());
        assertEquals(null, jdbc.queryForObject("SELECT delegation_grant_id FROM iam_role_assignment", Long.class));
    }

    @Test
    void restrictedActorMustBindItsOwnDelegation() {
        governed = false;
        delegation(61, 101);

        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.TENANT, batch(null, "700")));

        assertEquals(0, count());
        assertEquals(IamReasonCode.ACTION_DENIED, code("delegationGrantId", batch(null, "700")));
    }

    @Test
    void delegatedAssignmentWithinTheCeilingIsAccepted() {
        governed = false;
        delegation(61, 101);

        var created = service.create(AuthorizationDomain.TENANT, batch("61", "700"));

        assertEquals("0", created.version());
        assertEquals(61L, jdbc.queryForObject("SELECT delegation_grant_id FROM iam_role_assignment", Long.class));
    }

    @Test
    void borrowedDelegationIsRejectedEvenForGovernedActor() {
        delegation(62, 102);

        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.TENANT, batch("62", "700")));

        assertEquals(0, count());
        assertEquals(IamReasonCode.ACTION_DENIED, code("delegationGrantId", batch("62", "700")));
    }

    @Test
    void restrictedActorCannotStitchTwoDelegationsInOneBatch() {
        governed = false;
        delegation(61, 101);
        delegation(63, 101);
        var stitched = new AssignmentBatchInput(List.of(batch("61", "700").items().getFirst(),
                batch("63", "700").items().getFirst()));

        BizException failure = assertThrows(BizException.class,
                () -> service.create(AuthorizationDomain.TENANT, stitched));

        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
        assertEquals(0, count());
    }

    @Test
    void scopeOutsideTheCeilingIsRejected() {
        governed = false;
        delegation(61, 101);

        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.TENANT, batch("61", "710")));

        assertEquals(0, count());
        assertEquals(IamReasonCode.ACTION_DENIED, code("scopeBindings", batch("61", "710")));
    }

    @Test
    void missingCeilingForAGrantedActionIsRejected() {
        governed = false;
        delegation(61, 101);
        jdbc.update("DELETE FROM iam_delegation_action_ceiling WHERE delegation_id=61");

        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.TENANT, batch("61", "700")));

        assertEquals(0, count());
        assertEquals(IamReasonCode.ACTION_DENIED, code("delegationGrantId", batch("61", "700")));
    }

    @Test
    void recipientOutsideTheDelegationIsRejected() {
        governed = false;
        delegation(61, 101);
        jdbc.update("DELETE FROM iam_delegation_recipient_member WHERE delegation_id=61");

        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.TENANT, batch("61", "700")));

        assertEquals(0, count());
        assertEquals(IamReasonCode.ACTION_DENIED, code("subject", batch("61", "700")));
    }

    @Test
    void recipientDepartmentCoversTheMemberOnlyWhenDescendantsAreIncluded() {
        governed = false;
        delegation(61, 101);
        jdbc.update("DELETE FROM iam_delegation_recipient_member WHERE delegation_id=61");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,102,710,TRUE)");
        jdbc.update("INSERT INTO iam_delegation_recipient_department VALUES (61,10,700,FALSE)");

        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.TENANT, batch("61", "700")));

        jdbc.update("UPDATE iam_delegation_recipient_department SET include_descendants=TRUE WHERE delegation_id=61");
        service.create(AuthorizationDomain.TENANT, batch("61", "700"));

        assertEquals(1, count());
    }

    @Test
    void previewReportsTheSameRestrictionsWithoutWriting() {
        governed = false;
        delegation(61, 101);

        var missingSource = service.preview(AuthorizationDomain.TENANT, batch(null, "700"));
        var withinCeiling = service.preview(AuthorizationDomain.TENANT, batch("61", "700"));

        assertFalse(missingSource.valid());
        assertTrue(withinCeiling.valid());
        assertEquals(0, count());
    }

    @Test
    void platformUpgradePreservesIdentityValidityAndCreationTime() {
        platformUpgradeFixture();
        var before=jdbc.queryForMap("SELECT id,platform_member_id,valid_from,valid_until,created_at FROM iam_role_assignment WHERE id=81");
        var input=upgradeInput("0");
        assertTrue(service.previewUpgrade(input).valid());
        service.upgrade(input);
        assertEquals(before,jdbc.queryForMap("SELECT id,platform_member_id,valid_from,valid_until,created_at FROM iam_role_assignment WHERE id=81"));
        assertEquals(35L,jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=81",Long.class));
        assertEquals(1L,jdbc.queryForObject("SELECT version FROM iam_role_assignment WHERE id=81",Long.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iam_authorization_audit WHERE assignment_id=81 AND change_type='UPDATE'",Integer.class));
        assertEquals(IamReasonCode.REVISION_CONFLICT.getCode(),assertThrows(BizException.class,()->service.upgrade(input)).getCode());
    }

    @Test
    void platformUpgradeRejectsExpiredAndRevokedAssignmentsWithoutRestoringThem() {
        platformUpgradeFixture();
        jdbc.update("UPDATE iam_role_assignment SET valid_until=TIMESTAMP '2000-01-01 00:00:00' WHERE id=81");
        assertFalse(service.previewUpgrade(upgradeInput("0")).valid());
        assertThrows(BizException.class,()->service.upgrade(upgradeInput("0")));
        jdbc.update("UPDATE iam_role_assignment SET valid_until=NULL,status='REVOKED' WHERE id=81");
        assertFalse(service.previewUpgrade(upgradeInput("0")).valid());
        assertEquals(34L,jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=81",Long.class));
    }

    @Test
    void platformUpgradeRollsBackWholeBatchOnConcurrentVersionConflict() {
        platformUpgradeFixture();
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,revision_id,revision_kind,scope_bindings,status,source,version) VALUES(82,'PLATFORM','MEMBER',1002,34,'PLATFORM_CUSTOM','{}','ACTIVE','MANUAL',2)");
        var input=new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput(new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM,"35"),List.of(
                new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeItem("81","0",null),
                new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeItem("82","0",null)));
        assertEquals(IamReasonCode.REVISION_CONFLICT.getCode(),assertThrows(BizException.class,()->service.upgrade(input)).getCode());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE revision_id=34",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM iam_authorization_audit",Integer.class));
    }

    @Test
    void restrictedUpgradeRequiresTargetAlreadyAllowedBySameDelegation() {
        platformUpgradeFixture();platformUpgradeDelegation();governed=false;
        assertFalse(service.previewUpgrade(upgradeInput("0")).valid());
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES(61,35,'PLATFORM_CUSTOM')");
        assertTrue(service.previewUpgrade(upgradeInput("0")).valid());
        service.upgrade(upgradeInput("0"));
        assertEquals(61L,jdbc.queryForObject("SELECT delegation_grant_id FROM iam_role_assignment WHERE id=81",Long.class));
    }

    @Test
    void restrictedUpgradeRechecksSourceAfterPreviewAndCannotUpgradeDirectRecords() {
        platformUpgradeFixture();platformUpgradeDelegation();governed=false;
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES(61,35,'PLATFORM_CUSTOM')");
        assertTrue(service.previewUpgrade(upgradeInput("0")).valid());
        jdbc.update("UPDATE iam_delegation_grant SET status='REVOKED' WHERE id=61");
        assertThrows(BizException.class,()->service.upgrade(upgradeInput("0")));
        assertEquals(34L,jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=81",Long.class));
        jdbc.update("UPDATE iam_role_assignment SET delegation_grant_id=NULL WHERE id=81");
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(),assertThrows(BizException.class,()->service.previewUpgrade(upgradeInput("0"))).getCode());
    }

    private void platformUpgradeDelegation() {
        jdbc.update("INSERT INTO iam_delegation_grant(id,domain,platform_administrator_id,assignment_duration_mode,max_assignment_duration_seconds,max_assignment_duration_nanos,status,version) VALUES(61,'PLATFORM',1001,'UNLIMITED',NULL,0,'ACTIVE',0)");
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES(61,34,'PLATFORM_CUSTOM')");
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES(61,'PLATFORM',NULL,1002,NULL)");
        jdbc.update("INSERT INTO iam_delegation_action_ceiling VALUES(61,55,'[{\"kind\":\"ALL\"}]','{}')");
        jdbc.update("UPDATE iam_role_assignment SET delegation_grant_id=61 WHERE id=81");
    }


    @Test
    void onlyCurrentAdministratorMayAssignSystemRoleDirectlyToMembers() {
        platformUpgradeFixture();
        jdbc.update("INSERT INTO iam_role_definition(id,domain,kind,code,name,enabled) VALUES(25,'PLATFORM','SYSTEM',?,'超级管理员',TRUE)",
                com.ingot.framework.commons.constants.RoleConstants.ROLE_ADMIN_CODE);
        jdbc.update("INSERT INTO iam_role_revision(id,role_id,kind,revision) VALUES(36,25,'SYSTEM',1)");
        var direct = new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "1002"),
                new RoleRevisionRef(RoleKind.SYSTEM, "36"), Map.of(), null, null, null);
        assertFalse(service.preview(AuthorizationDomain.PLATFORM, new AssignmentBatchInput(List.of(direct))).valid());
        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.PLATFORM,
                new AssignmentBatchInput(List.of(direct))));
        administrator = true;
        assertTrue(service.preview(AuthorizationDomain.PLATFORM, new AssignmentBatchInput(List.of(direct))).valid());
        var delegated = new AssignmentInput(direct.subject(), direct.roleRevisionRef(), Map.of(), null, null, "61");
        assertFalse(service.preview(AuthorizationDomain.PLATFORM, new AssignmentBatchInput(List.of(delegated))).valid());
        var group = new AssignmentInput(new SubjectRef(SubjectType.GROUP, "1002"), direct.roleRevisionRef(),
                Map.of(), null, null, null);
        jdbc.execute("CREATE TABLE iam_platform_group(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT, member_id BIGINT)");
        jdbc.update("INSERT INTO iam_platform_group VALUES(1002)");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(1002,1002)");
        assertFalse(service.preview(AuthorizationDomain.PLATFORM, new AssignmentBatchInput(List.of(group))).valid());
    }

    @Test
    void memberCreationAcceptsExplicitPublishedOlderVersionButNotWrongRole() {
        platformUpgradeFixture();
        jdbc.update("DELETE FROM iam_role_assignment WHERE id=81");
        var draft = new com.ingot.framework.commons.model.iam.MemberRoleAssignmentDraft("24",
                new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "34"), Map.of(), null, null);
        service.grantMemberRoleAssignments("1002", List.of(draft));
        assertEquals(34L, jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment", Long.class));
        var wrong = new com.ingot.framework.commons.model.iam.MemberRoleAssignmentDraft("22",
                draft.roleRevisionRef(), Map.of(), null, null);
        assertThrows(BizException.class, () -> service.grantMemberRoleAssignments("1002", List.of(wrong)));
    }

    @Test
    void memberDeltaRejectsDelegationOnlyAndForeignOrStaleRecordWithoutWrites() {
        platformUpgradeFixture();
        var removal = new com.ingot.framework.commons.model.iam.MemberRoleChanges(List.of(), List.of(),
                List.of(new com.ingot.framework.commons.model.iam.MemberRoleRemoval("81", "0")));
        governed = false;
        assertThrows(BizException.class, () -> service.previewMemberRoleChanges("1002", removal));
        governed = true;
        assertThrows(BizException.class, () -> service.previewMemberRoleChanges("1001", removal));
        var stale = new com.ingot.framework.commons.model.iam.MemberRoleChanges(List.of(), List.of(),
                List.of(new com.ingot.framework.commons.model.iam.MemberRoleRemoval("81", "9")));
        assertThrows(BizException.class, () -> service.previewMemberRoleChanges("1002", stale));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_role_assignment WHERE id=81", String.class));
    }

    private void platformUpgradeFixture() {
        jdbc.update("INSERT INTO iam_platform_member VALUES(1002,2,'ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_role_definition(id,domain,kind,code,name,enabled,version) VALUES(24,'PLATFORM','PLATFORM_CUSTOM','ops','运维',TRUE,0)");
        jdbc.update("INSERT INTO iam_role_revision(id,role_id,kind,revision) VALUES(34,24,'PLATFORM_CUSTOM',1),(35,24,'PLATFORM_CUSTOM',2)");
        jdbc.update("INSERT INTO iam_role_grant VALUES(34,55,'[{\"kind\":\"ALL\"}]'),(35,55,'[{\"kind\":\"ALL\"}]')");
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,revision_id,revision_kind,scope_bindings,valid_from,status,source,created_at,version) VALUES(81,'PLATFORM','MEMBER',1002,34,'PLATFORM_CUSTOM','{}',TIMESTAMP '2000-01-01 00:00:00','ACTIVE','MANUAL',TIMESTAMP '2000-01-02 00:00:00',0)");
        var context=new AuthorizationContext(AuthorizationDomain.PLATFORM,null,"1","1001");
        var user=InUser.stateless(1L,null,"web","standard",UserTypeEnum.ADMIN.getValue(),"account",List.of(),List.of(),Map.of()).toBuilder().authorizationContext(context).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,List.of()));
    }
    private com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput upgradeInput(String version) {
        return new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput(new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM,"35"),
                List.of(new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeItem("81",version,null)));
    }

    /**
     * 建立一条委派：允许角色版本 32、接收成员 102，操作 12 的范围上限是部门 700 且不连带下级。
     *
     * @param id 委派 ID
     * @param administratorId 持有该委派的管理员成员 ID
     */
    private void delegation(long id, long administratorId) {
        jdbc.update("INSERT INTO iam_delegation_grant(id,domain,tenant_id,tenant_administrator_id,valid_from,"
                        + "max_assignment_duration_seconds,max_assignment_duration_nanos,status,version)"
                        + " VALUES (?,'TENANT',10,?,TIMESTAMP '2000-01-01 00:00:00',86400,0,'ACTIVE',0)",
                id, administratorId);
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES (?,32,'TENANT_CUSTOM')", id);
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES (?,'TENANT',10,NULL,102)", id);
        jdbc.update("INSERT INTO iam_delegation_action_ceiling VALUES (?,12,?,?)", id, MANAGED_DEPARTMENTS,
                "{\"scope\":{\"kind\":\"DEPARTMENTS\",\"ids\":[\"700\"]}}");
    }

    private AssignmentBatchInput batch(String delegationId, String... departmentIds) {
        Map<String, ScopeBinding> bindings = Map.of("scope",
                new ScopeBinding(ScopeBindingKind.DEPARTMENTS, List.of(departmentIds)));
        return new AssignmentBatchInput(List.of(new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "102"),
                new RoleRevisionRef(RoleKind.TENANT_CUSTOM, "32"), bindings, Instant.now(),
                Instant.now().plus(Duration.ofHours(1)), delegationId)));
    }

    /** 用预览取得指定字段上的拒绝原因，避免只断言整批异常码。 */
    private IamReasonCode code(String path, AssignmentBatchInput input) {
        return service.preview(AuthorizationDomain.TENANT, input).errors().stream()
                .filter(issue -> issue.path().equals(path))
                .map(ValidationIssue::code)
                .findFirst()
                .orElse(null);
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment", Integer.class);
    }

    private void authenticate() {
        var user = InUser.stateless(1L, 10L, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder().authorizationContext(TENANT).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
