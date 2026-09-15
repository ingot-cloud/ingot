package com.ingot.cloud.iam.identity;

import java.util.concurrent.atomic.AtomicLong;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.TenantCreateInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证组织初始化计划由服务器目录生成，不接受客户端指定治理版本或任意应用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class InitializationCatalogTest {
    private JdbcTemplate jdbc;
    private InitializationCatalog catalog;

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("identity/initialization.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        catalog = new InitializationCatalog(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.catalog(dataSource));
    }

    @Test
    void previewUsesBaselineApplicationsAndDoesNotExposeRevisionIds() {
        var preview = catalog.preview(input(null));
        assertEquals("研发组织", preview.name());
        assertEquals("2", preview.ownerAccountId());
        assertEquals("组织所有者", preview.ownerDisplayName());
        assertEquals(1, preview.applications().size());
        assertEquals("iam-tenant", preview.applications().getFirst().code());
        var json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(preview);
        assertFalse(json.toString().contains("governance"));
        assertFalse(json.has("governanceRevisionId"));
    }

    @Test
    void planAllocatesServerIdsAndReadsCatalogRevisions() {
        var ids = new AtomicLong(500);
        var plan = catalog.plan(input(null), ids::incrementAndGet);
        assertEquals(11L, plan.governanceRevisionId());
        assertEquals(21L, plan.directoryRevisionId());
        assertEquals(22L, plan.fieldRevisionId());
        assertEquals(1, plan.applications().size());
        assertEquals(1L, plan.applications().getFirst().applicationId());
        assertNotEquals(1L, plan.tenantId());
        assertEquals("研发组织", plan.name());
    }

    @Test
    void planApplicationsReplaceBaselineAndRejectPlatformMembers() {
        jdbc.update("INSERT INTO iam_application(id,domain,code,name,baseline) VALUES (3,'TENANT','extra','Extra',FALSE)");
        jdbc.update("INSERT INTO iam_plan(id,name) VALUES (9,'基础套餐')");
        jdbc.update("INSERT INTO iam_plan_application(plan_id,application_id) VALUES (9,3)");
        var preview = catalog.preview(input("9"));
        assertEquals("extra", preview.applications().getFirst().code());
        jdbc.update("INSERT INTO iam_plan_application(plan_id,application_id) VALUES (9,2)");
        assertEquals(IamReasonCode.APPLICATION_UNAVAILABLE.getCode(),
                assertThrows(BizException.class, () -> catalog.preview(input("9"))).getCode());
    }

    @Test
    void missingBaselineOrAmbiguousGovernanceFailsClosed() {
        jdbc.update("UPDATE iam_application SET baseline=FALSE WHERE id=1");
        assertEquals(IamReasonCode.APPLICATION_UNAVAILABLE.getCode(),
                assertThrows(BizException.class, () -> catalog.preview(input(null))).getCode());
        jdbc.update("UPDATE iam_application SET baseline=TRUE WHERE id=1");
        jdbc.update("INSERT INTO iam_role_definition(id,domain,kind,code,name) VALUES (4,'TENANT','SYSTEM','dup','重复')");
        assertEquals(IamReasonCode.ROLE_REVISION_UNAVAILABLE.getCode(),
                assertThrows(BizException.class, () -> catalog.preview(input(null))).getCode());
    }

    @Test
    void disabledOwnerCannotBeSelected() {
        jdbc.update("UPDATE iam_account SET enabled=FALSE WHERE id=2");
        assertEquals(IamReasonCode.IDENTITY_INVALID.getCode(),
                assertThrows(BizException.class, () -> catalog.preview(input(null))).getCode());
    }

    private static TenantCreateInput input(String planId) {
        return new TenantCreateInput("研发组织", "2", null, null, null, planId);
    }
}
