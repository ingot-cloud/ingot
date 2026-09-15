package com.ingot.cloud.iam.identity;

import java.util.UUID;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>使用真实 JDBC 查询验证独立成员身份和失败关闭；MySQL 方言另由隔离数据库测试覆盖。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class ActiveIdentityServiceTest {
    private JdbcTemplate jdbc;
    private ActiveIdentityService service;
    private final AuthorizationContext platform = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    private final AuthorizationContext tenantA = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private final AuthorizationContext tenantB = new AuthorizationContext(AuthorizationDomain.TENANT, "20", "1", "201");

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16), version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT, status VARCHAR(16), version BIGINT)");
        jdbc.update("INSERT INTO iam_account VALUES (1,TRUE,NULL,7),(2,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_platform_member VALUES (1001,1,'ACTIVE',3)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,TRUE,NULL,5),(20,TRUE,NULL,6)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'ACTIVE',1),(201,1,20,'ACTIVE',2)");
        service = new ActiveIdentityService(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.identity(dataSource));
    }

    @Test
    void authenticatedAccountSelectsExactlyOneExplicitDomain() {
        assertEquals(platform, service.selectAuthenticated("1", AuthorizationDomain.PLATFORM, null).context());
        assertEquals(tenantA, service.selectAuthenticated("1", AuthorizationDomain.TENANT, "10").context());
        assertEquals(tenantB, service.selectAuthenticated("1", AuthorizationDomain.TENANT, "20").context());
    }

    @Test
    void identitySelectionRejectsImplicitOrMixedDomain() {
        assertThrows(BizException.class, () -> service.selectAuthenticated("1", null, null));
        assertThrows(BizException.class, () -> service.selectAuthenticated("1", AuthorizationDomain.TENANT, null));
        assertThrows(BizException.class, () -> service.selectAuthenticated("1", AuthorizationDomain.PLATFORM, "10"));
        assertThrows(BizException.class, () -> service.selectAuthenticated("1", AuthorizationDomain.TENANT, "10junk"));
        assertThrows(BizException.class, () -> service.selectAuthenticated("2", AuthorizationDomain.PLATFORM, null));
    }

    @Test
    void identitySelectionDoesNotReviveSuspendedOrDisabledMembership() {
        jdbc.update("UPDATE iam_platform_member SET status='SUSPENDED' WHERE id=1001");
        assertThrows(BizException.class, () -> service.selectAuthenticated("1", AuthorizationDomain.PLATFORM, null));
        assertEquals(tenantA, service.selectAuthenticated("1", AuthorizationDomain.TENANT, "10").context());
        jdbc.update("UPDATE iam_account SET enabled=FALSE WHERE id=1");
        assertThrows(BizException.class, () -> service.selectAuthenticated("1", AuthorizationDomain.TENANT, "10"));
    }

    @Test
    void accountOwnsIndependentPlatformAndTwoTenantIdentities() {
        assertEquals(platform, service.requireActive(platform).context());
        assertEquals(tenantA, service.requireActive(tenantA).context());
        assertEquals(tenantB, service.requireActive(tenantB).context());
        assertNull(service.requireActive(platform).tenantVersion());
        assertEquals("5", service.requireActive(tenantA).tenantVersion());
    }

    @Test
    void memberIdentifiersCannotSwitchDomainOrTenantOrAccount() {
        invalid(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "101"));
        invalid(new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "1001"));
        invalid(new AuthorizationContext(AuthorizationDomain.TENANT, "20", "1", "101"));
        invalid(new AuthorizationContext(AuthorizationDomain.TENANT, "10", "2", "101"));
    }

    @Test
    void suspendingPlatformMemberDoesNotSuspendTenantIdentities() {
        jdbc.update("UPDATE iam_platform_member SET status='SUSPENDED' WHERE id=1001");
        invalid(platform);
        assertNotNull(service.requireActive(tenantA));
        assertNotNull(service.requireActive(tenantB));
    }

    @Test
    void removingTenantMemberDoesNotDeleteOtherIdentities() {
        jdbc.update("UPDATE iam_tenant_member SET status='REMOVED' WHERE id=101");
        invalid(tenantA);
        assertNotNull(service.requireActive(platform));
        assertNotNull(service.requireActive(tenantB));
    }

    @Test
    void disablingAccountInvalidatesAllItsIdentities() {
        jdbc.update("UPDATE iam_account SET enabled=FALSE WHERE id=1");
        invalid(platform);
        invalid(tenantA);
        invalid(tenantB);
    }

    @Test
    void deletedAccountCannotAuthenticateThroughMembership() {
        jdbc.update("UPDATE iam_account SET deleted_at=CURRENT_TIMESTAMP WHERE id=1");
        invalid(platform);
        invalid(tenantA);
    }

    @Test
    void disabledOrDeletedTenantDoesNotAffectPlatformOrOtherTenant() {
        jdbc.update("UPDATE iam_tenant SET enabled=FALSE WHERE id=10");
        invalid(tenantA);
        assertNotNull(service.requireActive(platform));
        assertNotNull(service.requireActive(tenantB));
        jdbc.update("UPDATE iam_tenant SET enabled=TRUE,deleted_at=CURRENT_TIMESTAMP WHERE id=10");
        invalid(tenantA);
    }

    @Test
    void identityVersionsAreReadFreshWithoutCache() {
        assertEquals("7", service.requireActive(tenantA).accountVersion());
        jdbc.update("UPDATE iam_account SET version=8 WHERE id=1");
        jdbc.update("UPDATE iam_tenant_member SET version=9 WHERE id=101");
        var result = service.requireActive(tenantA);
        assertEquals("8", result.accountVersion());
        assertEquals("9", result.memberVersion());
    }

    @Test
    void malformedIdsCannotExploitDatabaseNumericCoercion() {
        for (String id : java.util.List.of("1suffix", "01", "1.0", "1 OR 1=1", "-1", "0", "18446744073709551616")) {
            invalid(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, id, "1001"));
        }
        invalid(null);
    }

    @Test
    void databaseFailureIsUnavailableRatherThanInvalidIdentityOrSuccess() {
        jdbc.execute("DROP TABLE iam_account");
        var failure = assertThrows(BizException.class, () -> service.requireActive(platform));
        assertEquals(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode(), failure.getCode());
        assertNotNull(failure.getCause());
    }

    private void invalid(AuthorizationContext context) {
        var failure = assertThrows(BizException.class, () -> service.requireActive(context));
        assertEquals(IamReasonCode.IDENTITY_INVALID.getCode(), failure.getCode());
    }
}
