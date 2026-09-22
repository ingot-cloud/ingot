package com.ingot.cloud.iam.catalog;

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

        var byName = catalog.listApplications(1, 20, "演示", null, null);
        assertEquals(1, byName.items().size());
        assertEquals("2", byName.items().getFirst().record().id());
        assertEquals("演示应用", byName.items().getFirst().record().name());

        var disabled = catalog.listApplications(1, 20, null, "DISABLED", null);
        assertEquals(1, disabled.items().size());
        assertEquals("3", disabled.items().getFirst().record().id());

        var combined = catalog.listApplications(1, 20, "应用", "ENABLED", null);
        assertEquals(1, combined.items().size());
        assertEquals("2", combined.items().getFirst().record().id());

        var baseline = catalog.listApplications(1, 20, null, null, true);
        assertEquals(1, baseline.items().size());
        assertEquals("2", baseline.items().getFirst().record().id());
        assertEquals(true, baseline.items().getFirst().record().baseline());

        var notBaseline = catalog.listApplications(1, 20, null, null, false);
        assertEquals(2, notBaseline.items().size());

        var none = catalog.listApplications(1, 20, "不存在", null, null);
        assertEquals(0, none.items().size());
        assertEquals(0, none.total());

        BizException invalid = assertThrows(BizException.class,
                () -> catalog.listApplications(1, 20, null, "ENABLE", null));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), invalid.getCode());
    }
}
