package com.ingot.cloud.iam.authorization;

import java.util.Set;
import java.util.UUID;

import com.ingot.cloud.iam.organization.GrantPresenceMemberGuard;
import com.ingot.cloud.iam.organization.MemberMutationGuard;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证写入路径按直接分配中的 ACTION 失败关闭，不因平台成员资格放行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class JdbcGrantPresenceAuthorizerTest {
    private JdbcTemplate jdbc;
    private JdbcGrantPresenceAuthorizer authorizer;
    private GrantPresenceMemberGuard guard;
    private static final AuthorizationContext PLATFORM = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_action(id BIGINT PRIMARY KEY, code VARCHAR(192), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_grant(revision_id BIGINT, action_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, tenant_member_id BIGINT, revision_id BIGINT,
                  status VARCHAR(16), valid_until TIMESTAMP)
                """);
        jdbc.update("INSERT INTO iam_action VALUES (1,?,TRUE),(2,?,TRUE),(3,?,TRUE)",
                IamAction.VALUE_PLATFORM_TENANT_CREATE, IamAction.VALUE_TENANT_MEMBER_STATUS,
                IamAction.VALUE_PLATFORM_MEMBER_STATUS);
        jdbc.update("INSERT INTO iam_role_grant VALUES (11,1),(12,2)");
        jdbc.update("INSERT INTO iam_role_assignment VALUES (21,'PLATFORM',NULL,'MEMBER',1001,NULL,11,'ACTIVE',NULL)");
        jdbc.update("INSERT INTO iam_role_assignment VALUES (22,'TENANT',10,'MEMBER',NULL,101,12,'ACTIVE',NULL)");
        authorizer = new JdbcGrantPresenceAuthorizer(dataSource);
        guard = new GrantPresenceMemberGuard(authorizer);
    }

    @Test
    void platformMemberDoesNotGainCreateTenantWithoutGrant() {
        jdbc.update("DELETE FROM iam_role_grant");
        BizException failure = assertThrows(BizException.class,
                () -> authorizer.require(PLATFORM, IamAction.PLATFORM_TENANT_CREATE));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
    }

    @Test
    void matchingDirectAssignmentAllowsExactActionOnly() {
        authorizer.require(PLATFORM, IamAction.PLATFORM_TENANT_CREATE);
        assertThrows(BizException.class, () -> authorizer.require(PLATFORM, IamAction.PLATFORM_MEMBER_STATUS));
        authorizer.require(TENANT, IamAction.TENANT_MEMBER_STATUS);
        assertThrows(BizException.class, () -> authorizer.require(TENANT, IamAction.PLATFORM_TENANT_CREATE));
    }

    @Test
    void revokedOrExpiredAssignmentIsDenied() {
        jdbc.update("UPDATE iam_role_assignment SET status='REVOKED' WHERE id=21");
        assertThrows(BizException.class, () -> authorizer.require(PLATFORM, IamAction.PLATFORM_TENANT_CREATE));
        jdbc.update("UPDATE iam_role_assignment SET status='ACTIVE',valid_until=TIMESTAMP '2000-01-01 00:00:00' WHERE id=21");
        assertThrows(BizException.class, () -> authorizer.require(PLATFORM, IamAction.PLATFORM_TENANT_CREATE));
    }

    @Test
    void memberGuardMapsOperationToDomainAction() {
        assertThrows(BizException.class, () -> guard.require(PLATFORM, MemberMutationGuard.Operation.CHANGE_STATUS,
                "1002", Set.of(), Set.of()));
        jdbc.update("INSERT INTO iam_role_grant VALUES (11,3)");
        guard.require(PLATFORM, MemberMutationGuard.Operation.CHANGE_STATUS, "1002", Set.of(), Set.of());
    }
}
