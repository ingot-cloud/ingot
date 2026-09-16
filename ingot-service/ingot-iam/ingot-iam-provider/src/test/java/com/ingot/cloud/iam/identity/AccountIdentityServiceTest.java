package com.ingot.cloud.iam.identity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>验证新模型登录选择单一成员，不把旧权限并入新身份。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AccountIdentityServiceTest {
    private static DataSource dataSource;
    private JdbcTemplate jdbc;
    private AccountIdentityService service;

    /** 整个类共用一个库与一份 MyBatis 配置，避免逐用例重建导致内存堆积。 */
    @BeforeAll
    static void source() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void database() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE iam_account(id BIGINT PRIMARY KEY, username VARCHAR(64), password_hash VARCHAR(300),
                  phone VARCHAR(32), email VARCHAR(128), enabled BOOLEAN, must_change_password BOOLEAN,
                  password_changed_at TIMESTAMP, last_login_at TIMESTAMP, version BIGINT, created_at TIMESTAMP,
                  updated_at TIMESTAMP, deleted_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16), version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, name VARCHAR(128), enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT, status VARCHAR(16), version BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT, is_primary BOOLEAN)");
        jdbc.execute("""
                CREATE TABLE account_lock_state(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, user_type VARCHAR(16),
                  locked BOOLEAN, lock_type VARCHAR(16), lock_reason_code VARCHAR(64), lock_reason_detail VARCHAR(255),
                  locked_at TIMESTAMP, locked_until TIMESTAMP, operator_id BIGINT, operator_name VARCHAR(64),
                  failed_login_count INT, last_failed_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO iam_account(id,username,password_hash,phone,enabled,must_change_password,version)
                VALUES (1,'alice','hash','13800000000',TRUE,FALSE,0)
                """);
        jdbc.update("INSERT INTO iam_platform_member VALUES (1001,1,'ACTIVE',3)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,'组织A',TRUE,NULL,1),(20,'组织B',TRUE,NULL,1)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'ACTIVE',1),(201,1,20,'ACTIVE',1)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,101,11,TRUE)");
        AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
        when(evaluator.evaluate(any())).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_PLATFORM_ACCOUNT_READ), List.of(), Map.of(), "1",
                Instant.now().plusSeconds(60)));
        service = new AccountIdentityService(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.accounts(dataSource),
                new ActiveIdentityService(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.identity(dataSource)),
                evaluator);
    }

    @Test
    void usernameLoginSelectsPlatformIdentityWithoutLegacyScopes() {
        UserDetailsRequest request = request("alice", null);
        var response = service.load(request).orElseThrow();
        assertEquals(1L, response.getId());
        assertEquals("hash", response.getPassword());
        assertEquals(AuthorizationDomain.PLATFORM, response.getAuthorizationContext().domain());
        assertEquals("1001", response.getAuthorizationContext().memberId());
        assertNull(response.getAuthorizationContext().tenantId());
        assertEquals(List.of(IamAction.VALUE_PLATFORM_ACCOUNT_READ), response.getScopes());
        assertEquals(List.of(), response.getDeptIds());
    }

    @Test
    void platformLoginDoesNotDiscloseTenantMemberships() {
        var response = service.load(request("alice", null)).orElseThrow();
        assertNull(response.getTenant());
        assertEquals(List.of(), response.getAllows());
    }

    @Test
    void phoneLoginSelectsExplicitTenantIdentityAndDepartments() {
        UserDetailsRequest request = request("13800000000", 10L);
        var response = service.load(request).orElseThrow();
        assertEquals(AuthorizationDomain.TENANT, response.getAuthorizationContext().domain());
        assertEquals("10", response.getAuthorizationContext().tenantId());
        assertEquals("101", response.getAuthorizationContext().memberId());
        assertEquals(List.of(11L), response.getDeptIds());
        assertEquals(2, response.getAllows().size());
    }

    @Test
    void declaredTenantDomainWithoutTenantOnlyReturnsMembershipCandidates() {
        UserDetailsRequest request = request("alice", null);
        request.setDomain(AuthorizationDomain.TENANT);
        var response = service.load(request).orElseThrow();
        assertNull(response.getAuthorizationContext());
        assertNull(response.getTenant());
        assertEquals(2, response.getAllows().size());
        assertEquals(List.of(), response.getDeptIds());
        assertEquals(List.of(), response.getScopes());
    }

    @Test
    void declaredPlatformDomainRejectsTenantScopedLogin() {
        UserDetailsRequest request = request("alice", 10L);
        request.setDomain(AuthorizationDomain.PLATFORM);
        BizException failure = assertThrows(BizException.class, () -> service.load(request));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), failure.getCode());
    }

    @Test
    void declaredTenantDomainWithTenantBuildsMemberContext() {
        UserDetailsRequest request = request("alice", 20L);
        request.setDomain(AuthorizationDomain.TENANT);
        var response = service.load(request).orElseThrow();
        assertEquals(AuthorizationDomain.TENANT, response.getAuthorizationContext().domain());
        assertEquals("20", response.getAuthorizationContext().tenantId());
        assertEquals("201", response.getAuthorizationContext().memberId());
    }

    @Test
    void declaredPlatformDomainStillHidesTenantMemberships() {
        UserDetailsRequest request = request("alice", null);
        request.setDomain(AuthorizationDomain.PLATFORM);
        var response = service.load(request).orElseThrow();
        assertEquals(AuthorizationDomain.PLATFORM, response.getAuthorizationContext().domain());
        assertEquals(List.of(), response.getAllows());
    }

    @Test
    void lockedAccountComesFromSecurityFrameworkLockState() {
        jdbc.update("INSERT INTO account_lock_state(user_id,user_type,locked,failed_login_count) VALUES (1,?,TRUE,0)",
                UserTypeEnum.ADMIN.getValue());
        var response = service.load(request("alice", null)).orElseThrow();
        assertTrue(response.getLocked());
        assertNull(response.getAuthorizationContext());
    }

    @Test
    void missingAccountDoesNotInventIdentity() {
        assertTrue(service.load(request("bob", null)).isEmpty());
    }

    @Test
    void existingAccountCannotFallBackWhenTargetMembershipMissing() {
        jdbc.update("UPDATE iam_platform_member SET status='SUSPENDED' WHERE id=1001");
        BizException failure = assertThrows(BizException.class, () -> service.load(request("alice", null)));
        assertEquals(IamReasonCode.IDENTITY_INVALID.getCode(), failure.getCode());
    }

    private static UserDetailsRequest request(String username, Long tenant) {
        UserDetailsRequest request = new UserDetailsRequest();
        request.setUsername(username);
        request.setUserType(UserTypeEnum.ADMIN);
        request.setTenant(tenant);
        return request;
    }
}
