package com.ingot.cloud.iam.identity;

import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.TenantCreateInput;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证组织创建 HTTP 编排先校验平台 ACTION，再使用服务器目录，不接受客户端治理版本。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantInitializationServiceTest {
    private JdbcTemplate jdbc;
    private TenantInitializationService service;
    private static final AuthorizationContext PLATFORM = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("identity/initialization.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        var identities = new ActiveIdentityService(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.identity(dataSource));
        service = new TenantInitializationService(new CurrentIdentityService(identities),
                (actor, action) -> {
                    if (action != IamAction.PLATFORM_TENANT_CREATE && action != IamAction.PLATFORM_TENANT_PREVIEW) {
                        throw new IllegalStateException(action.getCode());
                    }
                },
                new InitializationCatalog(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.catalog(dataSource)),
                new TenantInitializer(new DataSourceTransactionManager(dataSource), identities,
                        com.ingot.cloud.iam.persistence.IamMybatisTestAccess.tenantInit(dataSource),
                        com.ingot.cloud.iam.persistence.IamMybatisTestAccess.audits(dataSource)),
                new AtomicLong(800)::incrementAndGet);
        var user = InUser.stateless(1L, null, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                java.util.List.of(), java.util.List.of(), java.util.Map.of())
                .toBuilder().authorizationContext(PLATFORM).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, java.util.List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void previewDoesNotAllocateIdsOrWriteTenants() {
        var preview = service.preview(input());
        assertTrue(preview.valid());
        assertEquals("研发组织", preview.effectiveResult().name());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant", Integer.class));
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(preview).toString().contains("governanceRevisionId"));
    }

    @Test
    void createWritesMinimalOrganizationWithoutCopyingCatalog() {
        var created = service.create(input());
        assertEquals("802", created.id());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_definition", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM iam_application", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_app_entitlement", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_definition WHERE tenant_id IS NOT NULL", Integer.class));
    }

    @Test
    void invalidOwnerReturnsPreviewErrorWithoutWrite() {
        var preview = service.preview(new TenantCreateInput("研发组织", "99", null, null, null, null));
        assertFalse(preview.valid());
        assertEquals(1, preview.errors().size());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant", Integer.class));
    }

    private static TenantCreateInput input() {
        return new TenantCreateInput("研发组织", "2", null, null, null, null);
    }
}
