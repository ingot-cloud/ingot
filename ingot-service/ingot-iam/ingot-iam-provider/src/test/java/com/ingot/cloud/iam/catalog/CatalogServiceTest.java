package com.ingot.cloud.iam.catalog;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ApplicationDraft;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>验证创建应用目录项不会隐式写入开通。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CatalogServiceTest {
    private JdbcTemplate jdbc;
    private CatalogService catalog;

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE iam_application(id BIGINT PRIMARY KEY, code VARCHAR(64), domain VARCHAR(16),
                  name VARCHAR(128), description VARCHAR(512), icon VARCHAR(512), sort_order INT,
                  baseline BOOLEAN, enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("CREATE TABLE iam_tenant_app_entitlement(id BIGINT PRIMARY KEY, tenant_id BIGINT, application_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_resource(id BIGINT PRIMARY KEY, application_id BIGINT, code VARCHAR(192),
                  name VARCHAR(128), scope_capabilities VARCHAR(4096), field_capabilities VARCHAR(4096),
                  enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("""
                CREATE TABLE iam_action(id BIGINT PRIMARY KEY, application_id BIGINT, resource_id BIGINT,
                  code VARCHAR(192), name VARCHAR(128), enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("""
                CREATE TABLE iam_menu(id BIGINT PRIMARY KEY, application_id BIGINT, parent_id BIGINT,
                  name VARCHAR(128), path VARCHAR(256), view_path VARCHAR(256), route_name VARCHAR(128),
                  icon VARCHAR(128), kind VARCHAR(16), match_mode VARCHAR(16), access_mode VARCHAR(16),
                  sort_order INT, enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("CREATE TABLE iam_menu_action(application_id BIGINT, menu_id BIGINT, action_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),actor_account_id BIGINT,
                  actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,target_type VARCHAR(64),target_id VARCHAR(128),
                  change_type VARCHAR(64),safe_before VARCHAR(4096),safe_after VARCHAR(4096),revisions VARCHAR(4096),
                  delegation_id BIGINT,assignment_id BIGINT,trace_id VARCHAR(128),occurred_at TIMESTAMP)
                """);
        IamAccess access = mock(IamAccess.class);
        ActiveIdentity actor = new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001"), "0", "0", null);
        when(access.require(any(), any())).thenReturn(actor);
        AtomicLong ids = new AtomicLong(100);
        when(access.nextId()).thenAnswer(invocation -> ids.incrementAndGet());
        catalog = new CatalogService(access, com.ingot.cloud.iam.persistence.IamMybatisTestAccess.audits(dataSource),
                new AuthorizationChangeNotifier(event -> {
                }), com.ingot.cloud.iam.persistence.IamMybatisTestAccess.catalogs(dataSource),
                new DataSourceTransactionManager(dataSource));
    }

    @Test
    void createApplicationDoesNotInsertEntitlement() {
        CreatedResource created = catalog.createApplication(new ApplicationDraft("demo", AuthorizationDomain.TENANT,
                "演示", null, null, 1, true));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_application", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_app_entitlement", Integer.class));
        assertEquals("0", created.version());
    }

    @Test
    void listApplicationsFiltersByNameAndStatus() {
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,sort_order,baseline,enabled,version)"
                + " VALUES (1,'gov','PLATFORM','平台治理',1,FALSE,TRUE,0),"
                + " (2,'demo','TENANT','演示应用',2,TRUE,TRUE,0),"
                + " (3,'off','TENANT','停用应用',3,FALSE,FALSE,0)");

        var byName = catalog.listApplications(1, 20, "TENANT", "演示", null, null);
        assertEquals(1, byName.items().size());
        assertEquals("2", byName.items().getFirst().record().id());
        assertEquals("演示应用", byName.items().getFirst().record().name());

        var disabled = catalog.listApplications(1, 20, "TENANT", null, "DISABLED", null);
        assertEquals(1, disabled.items().size());
        assertEquals("3", disabled.items().getFirst().record().id());

        var combined = catalog.listApplications(1, 20, "TENANT", "应用", "ENABLED", null);
        assertEquals(1, combined.items().size());
        assertEquals("2", combined.items().getFirst().record().id());

        var baseline = catalog.listApplications(1, 20, "TENANT", null, null, true);
        assertEquals(1, baseline.items().size());
        assertEquals("2", baseline.items().getFirst().record().id());
        assertEquals(true, baseline.items().getFirst().record().baseline());

        var notBaseline = catalog.listApplications(1, 20, "TENANT", null, null, false);
        assertEquals(1, notBaseline.items().size());
        assertEquals("3", notBaseline.items().getFirst().record().id());

        var platform = catalog.listApplications(1, 20, "PLATFORM", null, null, null);
        assertEquals(1, platform.items().size());
        assertEquals("1", platform.items().getFirst().record().id());

        var tenant = catalog.listApplications(1, 20, "TENANT", null, null, null);
        assertEquals(2, tenant.items().size());

        var none = catalog.listApplications(1, 20, "TENANT", "不存在", null, null);
        assertEquals(0, none.items().size());
        assertEquals(0, none.total());

        BizException invalid = assertThrows(BizException.class,
                () -> catalog.listApplications(1, 20, "TENANT", null, "ENABLE", null));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), invalid.getCode());

        BizException missingDomain = assertThrows(BizException.class,
                () -> catalog.listApplications(1, 20, null, null, null, null));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), missingDomain.getCode());

        BizException illegalDomain = assertThrows(BizException.class,
                () -> catalog.listApplications(1, 20, "MIXED", null, null, null));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), illegalDomain.getCode());
    }

    @Test
    void listPlansFiltersByNameAndStatus() {
        jdbc.execute("""
                CREATE TABLE iam_plan(id BIGINT PRIMARY KEY, name VARCHAR(128), description VARCHAR(512),
                  enabled BOOLEAN, version BIGINT DEFAULT 0)
                """);
        jdbc.execute("CREATE TABLE iam_plan_application(plan_id BIGINT, application_id BIGINT)");
        jdbc.update("INSERT INTO iam_plan(id,name,enabled,version)"
                + " VALUES (1,'基础套餐',TRUE,0),"
                + " (2,'演示套餐',TRUE,0),"
                + " (3,'停用套餐',FALSE,0)");

        var byName = catalog.listPlans(1, 20, "演示", null);
        assertEquals(1, byName.items().size());
        assertEquals("2", byName.items().getFirst().record().id());

        var disabled = catalog.listPlans(1, 20, null, "DISABLED");
        assertEquals(1, disabled.items().size());
        assertEquals("3", disabled.items().getFirst().record().id());

        var combined = catalog.listPlans(1, 20, "套餐", "ENABLED");
        assertEquals(2, combined.items().size());
        assertEquals(2, combined.total());

        var none = catalog.listPlans(1, 20, "不存在", null);
        assertEquals(0, none.items().size());

        BizException invalid = assertThrows(BizException.class,
                () -> catalog.listPlans(1, 20, null, "ENABLE"));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), invalid.getCode());
    }

    @Test
    void listResourcesFiltersByNameAndCode() {
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,sort_order,baseline,enabled,version)"
                + " VALUES (1,'demo','TENANT','演示',1,FALSE,TRUE,0)");
        jdbc.update("INSERT INTO iam_resource(id,application_id,code,name,scope_capabilities,field_capabilities,enabled,version)"
                + " VALUES (21,1,'account','全局账号','[]','[]',TRUE,0),"
                + " (22,1,'action','操作','[]','[]',TRUE,0),"
                + " (23,1,'group','用户组','[]','[]',TRUE,0)");

        var byName = catalog.listResources("1", 1, 20, "用户", null);
        assertEquals(1, byName.items().size());
        assertEquals("23", byName.items().getFirst().record().id());

        var byCode = catalog.listResources("1", 1, 20, null, "act");
        assertEquals(1, byCode.items().size());
        assertEquals("22", byCode.items().getFirst().record().id());

        var none = catalog.listResources("1", 1, 20, "不存在", "account");
        assertEquals(0, none.items().size());
    }

    @Test
    void listActionsFiltersByResourceNameAndIds() {
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,sort_order,baseline,enabled,version)"
                + " VALUES (1,'demo','TENANT','演示',1,FALSE,TRUE,0)");
        jdbc.update("INSERT INTO iam_action(id,application_id,resource_id,code,name,enabled,version)"
                + " VALUES (11,1,21,'demo:member:read','查看成员',TRUE,0),"
                + " (12,1,21,'demo:member:update','编辑成员',TRUE,0),"
                + " (13,1,22,'demo:dept:read','查看部门',TRUE,0)");

        var byResource = catalog.listActions("1", 1, 20, "21", null, null);
        assertEquals(2, byResource.items().size());
        assertEquals("11", byResource.items().getFirst().record().id());

        var byName = catalog.listActions("1", 1, 20, null, "部门", null);
        assertEquals(1, byName.items().size());
        assertEquals("13", byName.items().getFirst().record().id());

        var byIds = catalog.listActions("1", 1, 20, null, null, "12,13");
        assertEquals(2, byIds.items().size());
        assertEquals("12", byIds.items().getFirst().record().id());
        assertEquals("13", byIds.items().get(1).record().id());

        BizException invalid = assertThrows(BizException.class,
                () -> catalog.listActions("1", 1, 20, "x", null, null));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), invalid.getCode());
    }

    @Test
    void listMenuTreeNestsChildren() {
        jdbc.update("INSERT INTO iam_application(id,code,domain,name,sort_order,baseline,enabled,version)"
                + " VALUES (1,'demo','TENANT','演示',1,FALSE,TRUE,0)");
        jdbc.update("INSERT INTO iam_menu(id,application_id,parent_id,name,kind,match_mode,access_mode,sort_order,enabled,version)"
                + " VALUES (31,1,NULL,'组织','DIRECTORY','ANY','OPEN',1,TRUE,0),"
                + " (32,1,31,'成员','PAGE','ANY','ACTION',1,TRUE,0)");
        jdbc.update("INSERT INTO iam_menu_action(application_id,menu_id,action_id) VALUES (1,32,11)");

        var tree = catalog.listMenuTree("1");
        assertEquals(1, tree.size());
        assertEquals("31", tree.getFirst().record().id());
        assertEquals(1, tree.getFirst().children().size());
        assertEquals("32", tree.getFirst().children().getFirst().record().id());
        assertEquals(List.of("11"), tree.getFirst().children().getFirst().record().actionIds());
    }
}
