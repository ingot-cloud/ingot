package com.ingot.cloud.iam.application.catalog;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import java.util.List;

import com.ingot.cloud.iam.domain.catalog.AudienceKind;
import com.ingot.cloud.iam.application.catalog.model.AudienceView;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.cloud.iam.domain.catalog.EntitlementDraft;
import com.ingot.cloud.iam.application.catalog.model.EntitlementReplaceInput;
import com.ingot.cloud.iam.domain.catalog.EntitlementSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>验证开通列表带上应用目录名称、图标，并按名称筛选。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class EntitlementServiceTest {
    private JdbcTemplate jdbc;
    private IamAccess access;
    private EntitlementService service;

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, name VARCHAR(128), avatar VARCHAR(256),
                  owner_member_id BIGINT, plan_id BIGINT, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE iam_application(id BIGINT PRIMARY KEY, code VARCHAR(64), domain VARCHAR(16),
                  name VARCHAR(128), description VARCHAR(512), icon VARCHAR(512), sort_order INT,
                  baseline BOOLEAN, enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("""
                CREATE TABLE iam_tenant_app_entitlement(id BIGINT PRIMARY KEY, tenant_id BIGINT, application_id BIGINT,
                  enabled BOOLEAN, source VARCHAR(16), source_id BIGINT, valid_from TIMESTAMP, valid_until TIMESTAMP,
                  version BIGINT DEFAULT 0)
                """);
        jdbc.execute("""
                CREATE TABLE iam_app_audience(tenant_id BIGINT, application_id BIGINT, audience_kind VARCHAR(16),
                  version BIGINT)
                """);
        jdbc.execute("CREATE TABLE iam_audience_member(tenant_id BIGINT, application_id BIGINT, member_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_audience_department(tenant_id BIGINT, application_id BIGINT, department_id BIGINT,
                  include_descendants BOOLEAN)
                """);
        jdbc.execute("CREATE TABLE iam_audience_group(tenant_id BIGINT, application_id BIGINT, group_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, status VARCHAR(16),
                  display_name VARCHAR(128), avatar VARCHAR(256))
                """);
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(128))");
        jdbc.execute("""
                CREATE TABLE iam_resource(id BIGINT PRIMARY KEY, application_id BIGINT, code VARCHAR(192),
                  name VARCHAR(128), scope_capabilities VARCHAR(4096), field_capabilities VARCHAR(4096), field_policy BOOLEAN NOT NULL DEFAULT FALSE,
                  enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("""
                CREATE TABLE iam_action(id BIGINT PRIMARY KEY, application_id BIGINT, resource_id BIGINT,
                  code VARCHAR(192), name VARCHAR(128), enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("CREATE TABLE iam_menu(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE iam_menu_action(application_id BIGINT, menu_id BIGINT, action_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_plan(id BIGINT PRIMARY KEY, name VARCHAR(128), enabled BOOLEAN DEFAULT TRUE)");
        jdbc.execute("CREATE TABLE iam_plan_application(plan_id BIGINT, application_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_role_grant(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE iam_role_delta(id BIGINT PRIMARY KEY)");
        jdbc.execute("""
                CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),actor_account_id BIGINT,
                  actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,target_type VARCHAR(64),target_id VARCHAR(128),
                  change_type VARCHAR(64),safe_before VARCHAR(4096),safe_after VARCHAR(4096),revisions VARCHAR(4096),
                  delegation_id BIGINT,assignment_id BIGINT,trace_id VARCHAR(128),occurred_at TIMESTAMP)
                """);
        jdbc.update("INSERT INTO iam_tenant(id,name,enabled,version) VALUES (10,'组织',TRUE,0)");
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,baseline,enabled) VALUES (3,'iam-tenant','TENANT','组织治理',TRUE,TRUE)");
        jdbc.update("""
                INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,source,valid_from,version)
                VALUES (301,10,3,TRUE,'INITIALIZATION',TIMESTAMP '2026-01-02 00:00:00',0)
                """);
        access = mock(IamAccess.class);
        ActiveIdentity actor = new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001"), "0", "0", null);
        when(access.require(any(), any())).thenReturn(actor);
        AtomicLong ids = new AtomicLong(9000);
        when(access.nextId()).thenAnswer(invocation -> ids.incrementAndGet());
        service = new EntitlementService(access, com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess.audits(dataSource),
                new AuthorizationChangeNotifier(event -> {
                }), com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess.catalogs(dataSource),
                com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess.entitlements(dataSource),
                com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess.tenants(dataSource),
                new EntitlementResolver(com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess.catalog(dataSource),
                        com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess.catalogs(dataSource)),
                new DataSourceTransactionManager(dataSource));
    }

    @Test
    void platformListIncludesApplicationName() {
        var page = service.listForPlatform("10", 1, 20);
        assertEquals(1, page.items().size());
        assertEquals("3", page.items().getFirst().record().applicationId());
        assertEquals("组织治理", page.items().getFirst().record().applicationName());
        assertNull(page.items().getFirst().record().icon());
    }

    @Test
    void tenantListFiltersByNameAndReturnsIcon() {
        jdbc.update("UPDATE iam_application SET icon='ep:office-building' WHERE id=3");
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,baseline,enabled) VALUES (4,'extra','TENANT','业务应用',FALSE,TRUE)");
        jdbc.update("""
                INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,source,valid_from,version)
                VALUES (302,10,4,TRUE,'MANUAL',TIMESTAMP '2026-01-02 00:00:00',0)
                """);
        when(access.require(any(), any())).thenReturn(new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.TENANT, "10", "2", "1002"), "0", "0", null));
        var matched = service.listForTenant(1, 20, "业务");
        assertEquals(1, matched.items().size());
        assertEquals("业务应用", matched.items().getFirst().record().applicationName());
        assertNull(matched.items().getFirst().record().icon());
        var named = service.listForTenant(1, 20, "组织");
        assertEquals(1, named.items().size());
        assertEquals("ep:office-building", named.items().getFirst().record().icon());
        assertEquals(2, service.listForTenant(1, 20, " ").items().size());
    }

    @Test
    void tenantActionCatalogReturnsEnabledResourceTree() {
        jdbc.update("""
                INSERT INTO iam_resource(id,application_id,code,name,enabled) VALUES
                  (31,3,'member','成员',TRUE),(32,3,'hidden','停用资源',FALSE),(33,3,'empty','空资源',TRUE)
                """);
        jdbc.update("""
                INSERT INTO iam_action(id,application_id,resource_id,code,name,enabled) VALUES
                  (41,3,31,'iam-tenant:member:read','查看成员',TRUE),
                  (42,3,31,'iam-tenant:member:hidden','停用操作',FALSE),
                  (43,3,32,'iam-tenant:hidden:read','停用资源操作',TRUE)
                """);
        when(access.require(any(), any())).thenReturn(new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.TENANT, "10", "2", "1002"), "0", "0", null));
        var catalog = service.tenantActionCatalog("3");
        assertEquals("组织治理", catalog.applicationName());
        assertEquals(1, catalog.resources().size());
        assertEquals("成员", catalog.resources().getFirst().name());
        assertEquals(1, catalog.resources().getFirst().actions().size());
        assertEquals("查看成员", catalog.resources().getFirst().actions().getFirst().name());
    }

    @Test
    void tenantAudienceViewJoinsBoundNamesAndKeepsMissingRows() {
        jdbc.update("""
                INSERT INTO iam_app_audience(tenant_id,application_id,audience_kind,version) VALUES (10,3,'SELECTED',4)
                """);
        jdbc.update("""
                INSERT INTO iam_tenant_member(id,tenant_id,status,display_name,avatar)
                VALUES (50,10,'ACTIVE','张三','bucket/a.png')
                """);
        jdbc.update("""
                INSERT INTO iam_audience_member(tenant_id,application_id,member_id) VALUES (10,3,50),(10,3,99)
                """);
        jdbc.update("INSERT INTO iam_department(id,tenant_id,name) VALUES (70,10,'研发')");
        jdbc.update("""
                INSERT INTO iam_audience_department(tenant_id,application_id,department_id,include_descendants)
                VALUES (10,3,70,TRUE),(10,3,71,FALSE)
                """);
        jdbc.update("INSERT INTO iam_tenant_group(id,tenant_id,name) VALUES (80,10,'管理组')");
        jdbc.update("INSERT INTO iam_audience_group(tenant_id,application_id,group_id) VALUES (10,3,80),(10,3,81)");
        when(access.require(any(), any())).thenReturn(new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.TENANT, "10", "2", "1002"), "0", "0", null));
        var view = service.getAudience("3");
        assertEquals("4", view.version());
        assertEquals(AudienceKind.SELECTED, view.record().kind());
        assertEquals("50", view.record().members().getFirst().id());
        assertEquals("张三", view.record().members().getFirst().name());
        assertEquals("bucket/a.png", view.record().members().getFirst().avatar());
        assertEquals("99", view.record().members().get(1).id());
        assertEquals(AudienceView.RESTRICTED_NAME, view.record().members().get(1).name());
        assertNull(view.record().members().get(1).avatar());
        assertEquals("研发", view.record().departments().getFirst().name());
        assertTrue(view.record().departments().getFirst().includeDescendants());
        assertEquals(AudienceView.RESTRICTED_NAME, view.record().departments().get(1).name());
        assertFalse(view.record().departments().get(1).includeDescendants());
        assertEquals("管理组", view.record().groups().getFirst().name());
        assertEquals(AudienceView.RESTRICTED_NAME, view.record().groups().get(1).name());
        jdbc.update("UPDATE iam_app_audience SET audience_kind='ALL' WHERE application_id=3");
        var all = service.getAudience("3");
        assertEquals(AudienceKind.ALL, all.record().kind());
        assertTrue(all.record().members().isEmpty());
        assertTrue(all.record().departments().isEmpty());
        assertTrue(all.record().groups().isEmpty());
    }

    @Test
    void previewResolvesPlanUnionAndKeepsBaseline() {
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,baseline,enabled) VALUES (4,'extra','TENANT','业务应用',FALSE,TRUE)");
        jdbc.update("INSERT INTO iam_plan(id,name,enabled) VALUES (9,'基础套餐',TRUE)");
        jdbc.update("INSERT INTO iam_plan_application(plan_id,application_id) VALUES (9,4)");
        var preview = service.preview("10", new EntitlementReplaceInput("3:0:1", "9", List.of()));
        assertEquals(true, preview.valid());
        assertEquals(2, preview.effectiveResult().entitlements().size());
        assertTrue(preview.effectiveResult().entitlements().stream()
                .anyMatch(item -> "4".equals(item.applicationId()) && item.source() == EntitlementSource.PLAN));
        assertTrue(preview.effectiveResult().entitlements().stream()
                .anyMatch(item -> "3".equals(item.applicationId()) && item.source() == EntitlementSource.INITIALIZATION));
    }

    @Test
    void replaceWritesUnionAndPlanId() {
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,baseline,enabled) VALUES (4,'extra','TENANT','业务应用',FALSE,TRUE)");
        var replaced = service.replace("10", new EntitlementReplaceInput("3:0:1", null,
                List.of(new EntitlementDraft("4", ConfigurationStatus.ENABLED, null, null))));
        assertEquals(2, replaced.items().size());
        assertEquals(null, jdbc.queryForObject("SELECT plan_id FROM iam_tenant WHERE id=10", Long.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_tenant_app_entitlement WHERE tenant_id=10 AND application_id=4 AND source='MANUAL'",
                Integer.class));
    }
}
