package com.ingot.cloud.iam.role;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleCreateInput;
import com.ingot.framework.commons.model.iam.RoleDefinitionDraft;
import com.ingot.framework.commons.model.iam.RoleDelta;
import com.ingot.framework.commons.model.iam.RoleDeltaOperation;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleParameterDefinition;
import com.ingot.framework.commons.model.iam.RolePublishInput;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.UpgradeInput;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证角色版本形态与操作能力校验，以及升级只改本租户、本角色且重验通过的授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class RoleServiceTest {
    private JdbcTemplate jdbc;
    private RoleService service;
    /** 发号器由测试持有，便于在委派白名单里预先放开即将写入的版本 ID。 */
    private final AtomicLong ids = new AtomicLong(9000);
    /** 整个类共用一个库与一份 MyBatis 配置，避免逐用例重建导致内存堆积。 */
    private static DataSource dataSource;
    private static final AuthorizationContext TENANT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final String ALL = "[{\"kind\":\"ALL\"}]";
    private static final String MANAGED =
            "[{\"kind\":\"MANAGED_DEPARTMENTS\",\"parameterKey\":\"scope\",\"includeDescendants\":false}]";

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
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, owner_member_id BIGINT, enabled BOOLEAN,"
                + " deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT,"
                + " display_name VARCHAR(128), status VARCHAR(16), version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16),"
                + " version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_application(id BIGINT PRIMARY KEY, code VARCHAR(64), domain VARCHAR(16),"
                + " name VARCHAR(128), enabled BOOLEAN DEFAULT TRUE)");
        jdbc.execute("CREATE TABLE iam_resource(id BIGINT PRIMARY KEY, application_id BIGINT, code VARCHAR(64),"
                + " name VARCHAR(128), scope_capabilities VARCHAR(512), field_capabilities VARCHAR(512),"
                + " enabled BOOLEAN DEFAULT TRUE)");
        jdbc.execute("CREATE TABLE iam_action(id BIGINT PRIMARY KEY, application_id BIGINT, resource_id BIGINT,"
                + " code VARCHAR(192), name VARCHAR(128), enabled BOOLEAN DEFAULT TRUE)");
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,"
                + " kind VARCHAR(24), code VARCHAR(64), name VARCHAR(128), description VARCHAR(256),"
                + " group_name VARCHAR(64), enabled BOOLEAN DEFAULT TRUE, version BIGINT DEFAULT 0)");
        jdbc.execute("CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY, role_id BIGINT, kind VARCHAR(24),"
                + " revision BIGINT DEFAULT 1, base_revision_id BIGINT, metadata_overrides VARCHAR(1024),"
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
                  max_assignment_duration_nanos INT, status VARCHAR(16), version BIGINT DEFAULT 0,
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
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT,"
                + " name VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT,"
                + " is_primary BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(128),"
                + " description VARCHAR(256), version BIGINT DEFAULT 0, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT, group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT, group_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),"
                + "actor_account_id BIGINT,actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,"
                + "target_type VARCHAR(64),target_id VARCHAR(128),change_type VARCHAR(64),safe_before VARCHAR(4096),"
                + "safe_after VARCHAR(4096),revisions VARCHAR(4096),delegation_id BIGINT,assignment_id BIGINT,"
                + "trace_id VARCHAR(128),occurred_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_account VALUES (1,TRUE,NULL,0),(2,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,101,TRUE,NULL,0),(20,201,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'管理员','ACTIVE',0,NULL),"
                + "(102,2,10,'成员','ACTIVE',0,NULL)");
        // 租户应用的资源支持全部与管理部门范围；平台应用用于验证跨域引用被拒。
        jdbc.update("INSERT INTO iam_application VALUES (1,'app-tenant','TENANT','业务',TRUE),"
                + "(2,'app-platform','PLATFORM','平台',TRUE)");
        jdbc.update("INSERT INTO iam_resource VALUES (1,1,'order','订单',?,'[]',TRUE),"
                + "(2,2,'tenant','租户',?,'[]',TRUE)", "[\"ALL\",\"SELF\",\"MANAGED_DEPARTMENTS\"]", "[\"ALL\"]");
        jdbc.update("INSERT INTO iam_action VALUES (11,1,1,'tenant.order.read','读',TRUE),"
                + "(12,1,1,'tenant.order.update','改',TRUE),(13,1,1,'tenant.order.export','导',FALSE),"
                + "(21,2,2,'platform.tenant.read','平台读',TRUE)");
        // 共享角色 20 的基础版本 30（读）与新基础 31（读+改）。
        jdbc.update("INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name,enabled,version)"
                + " VALUES (20,'TENANT',NULL,'SHARED','shared','共享业务',TRUE,0)");
        jdbc.update("INSERT INTO iam_role_revision(id,role_id,kind,revision) VALUES (30,20,'SHARED',1),"
                + "(31,20,'SHARED',2)");
        jdbc.update("INSERT INTO iam_role_grant VALUES (30,11,?),(31,11,?),(31,12,?)", ALL, ALL, ALL);
        var identities = new ActiveIdentityService(IamMybatisTestAccess.identity(dataSource));
        var transactions = new DataSourceTransactionManager(dataSource);
        IamActionAuthorizer authorizer = (actor, action) -> new IamActionAuthorizer.Admission(true);
        ids.set(9000);
        var access = new IamAccess(new CurrentIdentityService(identities), authorizer, ids::incrementAndGet);
        service = new RoleService(access, IamMybatisTestAccess.audits(dataSource),
                new AuthorizationChangeNotifier(event -> { }), new RoleSynthesisCache(),
                new RoleGrantValidator(IamMybatisTestAccess.roles(dataSource)),
                IamMybatisTestAccess.delegationAdmission(dataSource), IamMybatisTestAccess.roles(dataSource),
                transactions);
        authenticate();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customRoleWithoutSharedBaseKeepsItsOwnGrants() {
        var created = service.create(AuthorizationDomain.TENANT, false, input(null,
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL)), List.of(), List.of(), null)));

        long revision = revisionOf(created.id());
        assertNull(jdbc.queryForObject("SELECT base_revision_id FROM iam_role_revision WHERE id=?", Long.class,
                revision));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_grant WHERE revision_id=?", Integer.class,
                revision));
    }

    @Test
    void customRoleWithSharedBaseOnlyStoresDeltas() {
        var created = service.create(AuthorizationDomain.TENANT, false, input("30",
                new RoleDefinitionDraft(List.of(), List.of(delta("12", ScopeKind.ALL)), List.of(), null)));

        long revision = revisionOf(created.id());
        assertEquals(30L, jdbc.queryForObject("SELECT base_revision_id FROM iam_role_revision WHERE id=?", Long.class,
                revision));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_grant WHERE revision_id=?", Integer.class,
                revision));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_delta WHERE revision_id=?", Integer.class,
                revision));
    }

    @Test
    void basedRoleRejectsOwnGrantsAndCustomRoleRejectsDeltas() {
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejects(input("30",
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL)), List.of(), List.of(), null))));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejects(input(null,
                new RoleDefinitionDraft(List.of(), List.of(delta("12", ScopeKind.ALL)), List.of(), null))));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_definition WHERE kind='TENANT_CUSTOM'",
                Integer.class));
    }

    @Test
    void sharedBaseMustBeASharedRevision() {
        var custom = service.create(AuthorizationDomain.TENANT, false, input(null,
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL)), List.of(), List.of(), null)));

        assertEquals(IamReasonCode.ROLE_REVISION_UNAVAILABLE.getCode(),
                rejects(input(Long.toString(revisionOf(custom.id())),
                        new RoleDefinitionDraft(List.of(), List.of(delta("12", ScopeKind.ALL)), List.of(), null))));
    }

    @Test
    void grantsOutsideTheRoleDomainAreRejected() {
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejects(input(null,
                new RoleDefinitionDraft(List.of(grant("21", ScopeKind.ALL)), List.of(), List.of(), null))));
    }

    @Test
    void disabledActionCannotBeGranted() {
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), rejects(input(null,
                new RoleDefinitionDraft(List.of(grant("13", ScopeKind.ALL)), List.of(), List.of(), null))));
    }

    @Test
    void unknownActionCannotBeGranted() {
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), rejects(input(null,
                new RoleDefinitionDraft(List.of(grant("999", ScopeKind.ALL)), List.of(), List.of(), null))));
    }

    @Test
    void scopeBeyondTheResourceCapabilityIsRejected() {
        jdbc.update("UPDATE iam_resource SET scope_capabilities=? WHERE id=1", "[\"SELF\"]");

        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejects(input(null,
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL)), List.of(), List.of(), null))));
    }

    @Test
    void managedDepartmentsNeedADeclaredParameter() {
        var undeclared = new RoleDefinitionDraft(List.of(managedGrant("11")), List.of(), List.of(), null);
        var declared = new RoleDefinitionDraft(List.of(managedGrant("11")), List.of(),
                List.of(new RoleParameterDefinition("scope", ScopeBindingKind.DEPARTMENTS)), null);

        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejects(input(null, undeclared)));
        service.create(AuthorizationDomain.TENANT, false, input(null, declared));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_parameter", Integer.class));
    }

    @Test
    void publishKeepsTheRoleShapeChosenAtCreation() {
        var created = service.create(AuthorizationDomain.TENANT, false, input(null,
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL)), List.of(), List.of(), null)));

        service.publish(AuthorizationDomain.TENANT, false, created.id(), new RolePublishInput("0",
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL), grant("12", ScopeKind.ALL)),
                        List.of(), List.of(), null)));

        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_revision WHERE role_id=?", Integer.class,
                Long.parseLong(created.id())));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_grant WHERE revision_id=?", Integer.class,
                revisionOf(created.id())));
    }

    @Test
    void previewReportsCapabilityIssuesWithTheirOwnCode() {
        var created = service.create(AuthorizationDomain.TENANT, false, input(null,
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL)), List.of(), List.of(), null)));

        var crossDomain = service.preview(AuthorizationDomain.TENANT, false, created.id(),
                new RoleDefinitionDraft(List.of(grant("21", ScopeKind.ALL)), List.of(), List.of(), null));
        var acceptable = service.preview(AuthorizationDomain.TENANT, false, created.id(),
                new RoleDefinitionDraft(List.of(grant("12", ScopeKind.ALL)), List.of(), List.of(), null));

        assertEquals(IamReasonCode.INVALID_ARGUMENT, crossDomain.errors().getFirst().code());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_revision WHERE role_id=?", Integer.class,
                Long.parseLong(created.id())));
        assertEquals(List.of(), acceptable.errors());
    }

    @Test
    void upgradeMovesOnlyTheSelectedAssignment() {
        based();
        assignment(81, 32, null, "ACTIVE", 10);
        assignment(82, 32, null, "ACTIVE", 10);

        service.upgrade("22", new UpgradeInput("0", "31", List.of(), List.of("81")));

        long moved = jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=81", Long.class);
        assertEquals(latestRevision(22), moved);
        assertEquals(32L, jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=82", Long.class));
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM iam_role_assignment WHERE id=81", Long.class));
    }

    @Test
    void upgradeRefusesAssignmentsOutsideTheTenantRoleOrLifecycle() {
        based();
        assignment(81, 32, null, "ACTIVE", 10);
        assignment(82, 32, null, "ACTIVE", 20);
        assignment(83, 33, null, "ACTIVE", 10);
        assignment(84, 32, null, "REVOKED", 10);

        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), rejectsUpgrade(List.of("82")));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejectsUpgrade(List.of("83")));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), rejectsUpgrade(List.of("84")));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), rejectsUpgrade(List.of("81", "999")));
        assertEquals(32L, jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=81", Long.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_revision WHERE role_id=22", Integer.class));
    }

    @Test
    void upgradeRequiresTheDelegationToAllowTheNewRevision() {
        based();
        assignment(81, 32, null, "ACTIVE", 10);
        assignment(85, 32, 61L, "ACTIVE", 10);
        delegation(61, 32);

        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), rejectsUpgrade(List.of("81", "85")));
        assertEquals(32L, jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=81", Long.class));

        // 委派显式放开升级后的版本，才允许把派生授权迁过去；版本 ID 由发号器给出，升级返回值回过头验证预测正确。
        long next = ids.get() + 1;
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES (61,?,'TENANT_CUSTOM')", next);
        var created = service.upgrade("22", new UpgradeInput("0", "31", List.of(), List.of("81", "85")));

        assertEquals(Long.toString(next), created.id());
        assertEquals(next,
                jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=85", Long.class));
    }

    @Test
    void upgradeRejectsWhenTheNewRevisionNeedsAnUnboundParameter() {
        based();
        jdbc.update("INSERT INTO iam_role_parameter VALUES (32,'scope','DEPARTMENTS')");
        jdbc.update("UPDATE iam_role_grant SET scopes=? WHERE revision_id=31 AND action_id=12", MANAGED);
        assignment(81, 32, null, "ACTIVE", 10);

        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejectsUpgrade(List.of("81")));
        assertEquals(32L, jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=81", Long.class));
    }

    @Test
    void roleWithoutSharedBaseCannotUpgrade() {
        var created = service.create(AuthorizationDomain.TENANT, false, input(null,
                new RoleDefinitionDraft(List.of(grant("11", ScopeKind.ALL)), List.of(), List.of(), null)));

        BizException failure = assertThrows(BizException.class, () -> service.upgrade(created.id(),
                new UpgradeInput("0", "31", List.of(), List.of())));

        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), failure.getCode());
    }

    /** 建立以共享版本 30 为基础的租户定制角色 22 及无关角色 23。 */
    private void based() {
        jdbc.update("INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name,enabled,version)"
                + " VALUES (22,'TENANT',10,'TENANT_CUSTOM','custom','定制',TRUE,0),"
                + "(23,'TENANT',10,'TENANT_CUSTOM','other','另一个',TRUE,0)");
        jdbc.update("INSERT INTO iam_role_revision(id,role_id,kind,revision,base_revision_id)"
                + " VALUES (32,22,'TENANT_CUSTOM',1,30),(33,23,'TENANT_CUSTOM',1,30)");
    }

    private void assignment(long id, long revisionId, Long delegationId, String status, long tenantId) {
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,revision_id,"
                        + "revision_kind,scope_bindings,delegation_grant_id,valid_from,valid_until,status,source,version)"
                        + " VALUES (?,'TENANT',?,'MEMBER',102,?,'TENANT_CUSTOM','{}',?,"
                        + "TIMESTAMP '2026-01-01 00:00:00',TIMESTAMP '2026-01-02 00:00:00',?,'MANUAL',0)",
                id, tenantId, revisionId, delegationId, status);
    }

    /** 建立由当前操作者持有、只放开指定版本的委派，操作 11/12 的范围上限为全部。 */
    private void delegation(long id, long allowedRevisionId) {
        jdbc.update("INSERT INTO iam_delegation_grant(id,domain,tenant_id,tenant_administrator_id,valid_from,"
                + "max_assignment_duration_seconds,max_assignment_duration_nanos,status,version)"
                + " VALUES (?,'TENANT',10,101,TIMESTAMP '2000-01-01 00:00:00',864000,0,'ACTIVE',0)", id);
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES (?,?,'TENANT_CUSTOM')", id, allowedRevisionId);
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES (?,'TENANT',10,NULL,102)", id);
        jdbc.update("INSERT INTO iam_delegation_action_ceiling VALUES (?,11,?,'{}'),(?,12,?,'{}')",
                id, ALL, id, ALL);
    }

    private static RoleCreateInput input(String baseRevisionId, RoleDefinitionDraft definition) {
        return new RoleCreateInput("custom-" + UUID.randomUUID(), "定制角色", null, null, RoleKind.TENANT_CUSTOM,
                baseRevisionId, definition);
    }

    private static ActionGrant grant(String actionId, ScopeKind kind) {
        return new ActionGrant(actionId, List.of(new ScopeExpression(kind, null, null)));
    }

    private static ActionGrant managedGrant(String actionId) {
        return new ActionGrant(actionId, List.of(new ScopeExpression(ScopeKind.MANAGED_DEPARTMENTS, "scope", false)));
    }

    private static RoleDelta delta(String actionId, ScopeKind kind) {
        return new RoleDelta(actionId, RoleDeltaOperation.ADD, List.of(new ScopeExpression(kind, null, null)));
    }

    private String rejects(RoleCreateInput input) {
        return assertThrows(BizException.class,
                () -> service.create(AuthorizationDomain.TENANT, false, input)).getCode();
    }

    private String rejectsUpgrade(List<String> assignmentIds) {
        return assertThrows(BizException.class, () -> service.upgrade("22",
                new UpgradeInput("0", "31", List.of(), assignmentIds))).getCode();
    }

    private long revisionOf(String roleId) {
        return jdbc.queryForObject("SELECT id FROM iam_role_revision WHERE role_id=? ORDER BY revision DESC LIMIT 1",
                Long.class, Long.parseLong(roleId));
    }

    private long latestRevision(long roleId) {
        return revisionOf(Long.toString(roleId));
    }

    @Test
    void listSharedRolesFiltersByNameAndStatus() {
        jdbc.update("INSERT INTO iam_platform_member(id,account_id,status,version) VALUES (1,1,'ACTIVE',0)");
        jdbc.update("INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name,enabled,version)"
                + " VALUES (41,NULL,NULL,'SHARED','alpha','演示共享',TRUE,0),"
                + " (42,NULL,NULL,'SHARED','beta','停用共享',FALSE,0)");
        authenticatePlatform();

        var byName = service.list(AuthorizationDomain.PLATFORM, true, 1, 20, "演示", null);
        assertEquals(1, byName.items().size());
        assertEquals("41", byName.items().getFirst().record().id());

        var disabled = service.list(AuthorizationDomain.PLATFORM, true, 1, 20, null, "DISABLED");
        assertEquals(1, disabled.items().size());
        assertEquals("42", disabled.items().getFirst().record().id());

        var enabled = service.list(AuthorizationDomain.PLATFORM, true, 1, 20, "共享", "ENABLED");
        assertEquals(2, enabled.total());

        BizException invalid = assertThrows(BizException.class,
                () -> service.list(AuthorizationDomain.PLATFORM, true, 1, 20, null, "ENABLE"));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), invalid.getCode());
    }

    private void authenticatePlatform() {
        var user = InUser.stateless(1L, null, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder()
                .authorizationContext(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }

    private void authenticate() {
        var user = InUser.stateless(1L, 10L, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder().authorizationContext(TENANT).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
