package com.ingot.cloud.iam.catalog;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.model.iam.ApplicationDraft;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>验证创建应用目录项不会隐式写入开通。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class JdbcCatalogServiceTest {
    private JdbcTemplate jdbc;
    private JdbcCatalogService catalog;

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
                  occurred_at TIMESTAMP)
                """);
        IamAccess access = mock(IamAccess.class);
        ActiveIdentity actor = new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001"), "0", "0", null);
        when(access.require(any(), any())).thenReturn(actor);
        AtomicLong ids = new AtomicLong(100);
        when(access.nextId()).thenAnswer(invocation -> ids.incrementAndGet());
        catalog = new JdbcCatalogService(access, com.ingot.cloud.iam.persistence.IamMybatisTestAccess.audits(dataSource),
                new AuthorizationChangeNotifier(event -> {
                }), dataSource, new DataSourceTransactionManager(dataSource));
    }

    @Test
    void createApplicationDoesNotInsertEntitlement() {
        CreatedResource created = catalog.createApplication(new ApplicationDraft("demo", AuthorizationDomain.TENANT,
                "演示", null, null, 1, true));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_application", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_app_entitlement", Integer.class));
        assertEquals("0", created.version());
    }
}
