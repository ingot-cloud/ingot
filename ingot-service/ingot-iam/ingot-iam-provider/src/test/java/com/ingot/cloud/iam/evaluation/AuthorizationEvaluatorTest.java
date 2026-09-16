package com.ingot.cloud.iam.evaluation;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.sql.DataSource;

import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证组展开并入有效授权、租户域缺少开通时失败关闭，以及快照期限与写操作的缓存边界。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuthorizationEvaluatorTest {
    private JdbcTemplate jdbc;
    private AuthorizationEvaluator evaluator;
    /** 整个类共用一个库，避免每个用例重建一份 MyBatis 配置。 */
    private static DataSource dataSource;
    private static final AuthorizationContext PLATFORM = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final Duration MAX_HOT_WINDOW = Duration.ofSeconds(30);

    @BeforeAll
    static void source() {
        // INIT 让每个连接都按 UTC 会话运行，与生产库一致，期限比较不受运行机器时区影响。
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;INIT=SET TIME ZONE 'UTC'", "sa", "");
    }

    @BeforeEach
    void database() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE iam_application(id BIGINT PRIMARY KEY, domain VARCHAR(16), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_action(id BIGINT PRIMARY KEY, application_id BIGINT, code VARCHAR(192), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY, enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY, role_id BIGINT, base_revision_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_role_grant(revision_id BIGINT, action_id BIGINT, scopes VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_role_delta(revision_id BIGINT, action_id BIGINT, operation VARCHAR(32), scopes VARCHAR(128))");
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, platform_group_id BIGINT,
                  tenant_member_id BIGINT, tenant_group_id BIGINT, revision_id BIGINT, status VARCHAR(16),
                  valid_from TIMESTAMP, valid_until TIMESTAMP, delegation_grant_id BIGINT, scope_bindings VARCHAR(512))
                """);
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT, group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY, status VARCHAR(16), valid_from TIMESTAMP, valid_until TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_delegation_role_revision(delegation_id BIGINT, revision_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT, tenant_id BIGINT,"
                + " platform_member_id BIGINT, tenant_member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_department(delegation_id BIGINT, tenant_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_delegation_action_ceiling(delegation_id BIGINT, action_id BIGINT,"
                + " scopes VARCHAR(128), scope_bindings VARCHAR(512))");
        jdbc.execute("""
                CREATE TABLE iam_tenant_app_entitlement(tenant_id BIGINT, application_id BIGINT, enabled BOOLEAN,
                  valid_from TIMESTAMP, valid_until TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_app_audience(tenant_id BIGINT, application_id BIGINT, enabled BOOLEAN, audience_kind VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_audience_member(tenant_id BIGINT, application_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_audience_group(tenant_id BIGINT, application_id BIGINT, group_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_audience_department(tenant_id BIGINT, application_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, department_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT, tenant_id BIGINT, parent_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT, group_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.update("INSERT INTO iam_application VALUES (1,'PLATFORM',TRUE),(2,'TENANT',TRUE)");
        jdbc.update("INSERT INTO iam_action VALUES (11,1,?,TRUE),(12,2,?,TRUE)",
                IamAction.VALUE_PLATFORM_TENANT_CREATE, IamAction.VALUE_TENANT_MEMBER_STATUS);
        jdbc.update("INSERT INTO iam_role_definition VALUES (21,TRUE),(22,TRUE)");
        jdbc.update("INSERT INTO iam_role_revision VALUES (31,21,NULL),(32,22,NULL)");
        jdbc.update("INSERT INTO iam_role_grant VALUES (31,11,'[]'),(32,12,'[]')");
        jdbc.update("""
                INSERT INTO iam_role_assignment VALUES
                (41,'PLATFORM',NULL,'GROUP',NULL,501,NULL,NULL,31,'ACTIVE',TIMESTAMP '2000-01-01 00:00:00',NULL,NULL,'{}'),
                (42,'TENANT',10,'MEMBER',NULL,NULL,101,NULL,32,'ACTIVE',TIMESTAMP '2000-01-01 00:00:00',NULL,NULL,'{}')
                """);
        jdbc.update("INSERT INTO iam_platform_group_member VALUES (501,1001)");
        evaluator = com.ingot.cloud.iam.persistence.IamMybatisTestAccess.evaluator(dataSource);
    }

    private void entitleTenantApplication() {
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,NULL)");
        jdbc.update("INSERT INTO iam_app_audience VALUES (10,2,TRUE,'ALL')");
    }

    /** 组织 10 的部门树为 700 → 710 → 720，成员 101 只任职最深的 720。 */
    private void departmentChainWithMemberAtLeaf() {
        jdbc.update("INSERT INTO iam_department VALUES (700,10,NULL),(710,10,700),(720,10,710)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,720,101)");
    }

    /**
     * 让平台组分配 41 来自委派 61：白名单含版本 31、接收人含成员 1001、上限覆盖操作 11。
     *
     * @param validUntil 委派截止；null 表示不限期
     */
    private void platformDelegationOnPlatformAssignment(LocalDateTime validUntil) {
        jdbc.update("INSERT INTO iam_delegation_grant VALUES (61,'ACTIVE',TIMESTAMP '2000-01-01 00:00:00',?)",
                validUntil);
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES (61,31)");
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES (61,NULL,1001,NULL)");
        jdbc.update("INSERT INTO iam_delegation_action_ceiling VALUES (61,11,'[]','{}')");
        jdbc.update("UPDATE iam_role_assignment SET delegation_grant_id=61 WHERE id=41");
    }

    /** 把租户成员 101 的直接分配换成组 502 的组主体分配。 */
    private void tenantGroupAssignmentOnly() {
        jdbc.update("UPDATE iam_role_assignment SET subject_type='GROUP',tenant_member_id=NULL,tenant_group_id=502"
                + " WHERE id=42");
    }

    @Test
    void groupAssignmentExpandsToMember() {
        evaluator.require(PLATFORM, IamAction.PLATFORM_TENANT_CREATE);
        assertTrue(evaluator.evaluate(PLATFORM).actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));
    }

    @Test
    void tenantActionWithoutEntitlementIsDenied() {
        BizException failure = assertThrows(BizException.class,
                () -> evaluator.require(TENANT, IamAction.TENANT_MEMBER_STATUS));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
        assertFalse(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void tenantEntitlementAndAudienceAllowAction() {
        entitleTenantApplication();
        evaluator.require(TENANT, IamAction.TENANT_MEMBER_STATUS);
    }

    @Test
    void memberDepartmentsScopeIsBoundFromGrant() {
        entitleTenantApplication();
        jdbc.update("UPDATE iam_role_grant SET scopes=? WHERE revision_id=32",
                "[{\"kind\":\"MEMBER_DEPARTMENTS\",\"includeDescendants\":true}]");
        var view = evaluator.evaluate(TENANT);
        assertTrue(view.actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
        assertTrue(view.scope(IamAction.VALUE_TENANT_MEMBER_STATUS).clauses().stream()
                .anyMatch(clause -> clause.memberDepartments() && clause.memberDepartmentDescendants()));
    }

    @Test
    void snapshotWithoutDeadlineUsesTheFullHotWindow() {
        Instant before = Instant.now();
        Instant expiresAt = evaluator.evaluate(PLATFORM).expiresAt();
        assertFalse(expiresAt.isBefore(before.plus(MAX_HOT_WINDOW)));
        assertTrue(expiresAt.isBefore(Instant.now().plus(MAX_HOT_WINDOW).plusSeconds(5)));
    }

    @Test
    void snapshotIsTruncatedToTheNearestAssignmentDeadline() {
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(9);
        jdbc.update("UPDATE iam_role_assignment SET valid_until=? WHERE id=41", deadline);
        assertEquals(deadline.toInstant(ZoneOffset.UTC), evaluator.evaluate(PLATFORM).expiresAt());
    }

    @Test
    void snapshotIsTruncatedToTheNearestDelegationDeadline() {
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(7);
        platformDelegationOnPlatformAssignment(deadline);
        var view = evaluator.evaluate(PLATFORM);
        assertTrue(view.actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));
        assertEquals(deadline.toInstant(ZoneOffset.UTC), view.expiresAt());
    }

    @Test
    void snapshotIsTruncatedToTheNearestEntitlementDeadline() {
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(11);
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,?)", deadline);
        jdbc.update("INSERT INTO iam_app_audience VALUES (10,2,TRUE,'ALL')");
        assertEquals(deadline.toInstant(ZoneOffset.UTC), evaluator.evaluate(TENANT).expiresAt());
    }

    @Test
    void unboundedEntitlementKeepsTheHotWindowDespiteABoundedSibling() {
        // 存在不限期开通时最近边界不成立，MIN 不能把不限期当成截止。
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,?)",
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(11));
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,NULL)");
        jdbc.update("INSERT INTO iam_app_audience VALUES (10,2,TRUE,'ALL')");
        assertFalse(evaluator.evaluate(TENANT).expiresAt().isBefore(Instant.now().plus(MAX_HOT_WINDOW).minusSeconds(5)));
    }

    @Test
    void expiredCacheEntryIsDiscardedInsteadOfServedOrRenewed() {
        var cache = new RecordingCache(evaluator::evaluateRaw);
        var cached = com.ingot.cloud.iam.persistence.IamMybatisTestAccess.evaluator(dataSource, cache);
        cache.seed(AuthorizationEvaluator.cacheKey(PLATFORM), new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_PLATFORM_TENANT_READ), List.of(), Map.of(), "stale",
                Instant.now().minusSeconds(1)));

        var view = cached.evaluate(PLATFORM);

        assertEquals(1, cache.loads());
        assertFalse(view.actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_READ));
        assertTrue(view.actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));
        assertTrue(view.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void mutatingActionIgnoresTheHotSnapshotWhileReadingActionAcceptsIt() {
        var cache = new RecordingCache(evaluator::evaluateRaw);
        var cached = com.ingot.cloud.iam.persistence.IamMybatisTestAccess.evaluator(dataSource, cache);
        cache.seed(AuthorizationEvaluator.cacheKey(TENANT), new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_TENANT_DIRECTORY_READ, IamAction.VALUE_TENANT_MEMBER_STATUS),
                List.of(), Map.of(), "stale", Instant.now().plus(MAX_HOT_WINDOW)));

        cached.require(TENANT, IamAction.TENANT_DIRECTORY_READ);
        BizException failure = assertThrows(BizException.class,
                () -> cached.require(TENANT, IamAction.TENANT_MEMBER_STATUS));

        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
        assertEquals(0, cache.loads());
    }

    @Test
    void groupFromAncestorDepartmentGrantsOnlyWhenDescendantsAreIncluded() {
        entitleTenantApplication();
        departmentChainWithMemberAtLeaf();
        tenantGroupAssignmentOnly();
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,502,700,FALSE)");

        assertFalse(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));

        jdbc.update("UPDATE iam_tenant_group_department SET include_descendants=TRUE WHERE group_id=502");

        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void groupFromExactDepartmentGrantsWithoutDescendants() {
        entitleTenantApplication();
        departmentChainWithMemberAtLeaf();
        tenantGroupAssignmentOnly();
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,502,720,FALSE)");

        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void leavingTheDepartmentRemovesTheDepartmentSourcedGroupGrant() {
        entitleTenantApplication();
        departmentChainWithMemberAtLeaf();
        tenantGroupAssignmentOnly();
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,502,700,TRUE)");
        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));

        jdbc.update("DELETE FROM iam_member_department WHERE tenant_id=10 AND member_id=101");

        assertFalse(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void explicitGroupMemberStillGrantsWithoutAnyDepartmentSource() {
        entitleTenantApplication();
        tenantGroupAssignmentOnly();
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (10,502,101)");

        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void audienceDepartmentReachesMembersOfDescendantDepartments() {
        departmentChainWithMemberAtLeaf();
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,NULL)");
        jdbc.update("INSERT INTO iam_app_audience VALUES (10,2,TRUE,'DEPARTMENT')");
        jdbc.update("INSERT INTO iam_audience_department VALUES (10,2,700,FALSE)");

        assertFalse(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));

        jdbc.update("UPDATE iam_audience_department SET include_descendants=TRUE");

        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void audienceGroupAcceptsMembersJoiningThroughADepartment() {
        departmentChainWithMemberAtLeaf();
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,NULL)");
        jdbc.update("INSERT INTO iam_app_audience VALUES (10,2,TRUE,'GROUP')");
        jdbc.update("INSERT INTO iam_audience_group VALUES (10,2,503)");
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,503,710,TRUE)");

        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void delegatedAssignmentStopsGrantingWhenTheRevisionLeavesTheWhitelist() {
        platformDelegationOnPlatformAssignment(null);
        assertTrue(evaluator.evaluate(PLATFORM).actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));

        jdbc.update("DELETE FROM iam_delegation_role_revision WHERE delegation_id=61 AND revision_id=31");

        assertFalse(evaluator.evaluate(PLATFORM).actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));
    }

    @Test
    void delegatedAssignmentStopsGrantingWhenTheMemberLeavesTheRecipients() {
        platformDelegationOnPlatformAssignment(null);
        assertTrue(evaluator.evaluate(PLATFORM).actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));

        jdbc.update("DELETE FROM iam_delegation_recipient_member WHERE delegation_id=61");

        assertFalse(evaluator.evaluate(PLATFORM).actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));
    }

    @Test
    void wideningTheGroupDoesNotBenefitMembersOutsideTheRecipients() {
        entitleTenantApplication();
        tenantGroupAssignmentOnly();
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (10,502,101)");
        jdbc.update("INSERT INTO iam_delegation_grant VALUES (62,'ACTIVE',TIMESTAMP '2000-01-01 00:00:00',NULL)");
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES (62,32)");
        jdbc.update("INSERT INTO iam_delegation_action_ceiling VALUES (62,12,'[]','{}')");
        jdbc.update("UPDATE iam_role_assignment SET delegation_grant_id=62 WHERE id=42");

        // 组把成员拉了进来，但委派接收名单里没有他，求值不得放行。
        assertFalse(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));

        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES (62,10,NULL,101)");

        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void recipientDepartmentReachesTheMemberOnlyWhenDescendantsAreIncluded() {
        entitleTenantApplication();
        departmentChainWithMemberAtLeaf();
        jdbc.update("INSERT INTO iam_delegation_grant VALUES (63,'ACTIVE',TIMESTAMP '2000-01-01 00:00:00',NULL)");
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES (63,32)");
        jdbc.update("INSERT INTO iam_delegation_action_ceiling VALUES (63,12,'[]','{}')");
        jdbc.update("INSERT INTO iam_delegation_recipient_department VALUES (63,10,700,FALSE)");
        jdbc.update("UPDATE iam_role_assignment SET delegation_grant_id=63 WHERE id=42");

        assertFalse(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));

        jdbc.update("UPDATE iam_delegation_recipient_department SET include_descendants=TRUE WHERE delegation_id=63");

        assertTrue(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void delegatedActionIsNotCountedAsGovernanceQualification() {
        assertTrue(evaluator.admit(PLATFORM, IamAction.PLATFORM_TENANT_CREATE).governed());

        platformDelegationOnPlatformAssignment(null);

        assertFalse(evaluator.admit(PLATFORM, IamAction.PLATFORM_TENANT_CREATE).governed());
    }

    /**
     * 记录加载次数的最小热缓存替身，行为对齐 L1 命中与 evict 后重载。
     */
    private static final class RecordingCache implements LayeredCache<String, AuthorizationEvaluator.AuthorizationView> {
        private final Map<String, AuthorizationEvaluator.AuthorizationView> entries = new LinkedHashMap<>();
        private final Function<String, AuthorizationEvaluator.AuthorizationView> loader;
        private int loads;

        private RecordingCache(Function<String, AuthorizationEvaluator.AuthorizationView> loader) {
            this.loader = loader;
        }

        private void seed(String key, AuthorizationEvaluator.AuthorizationView view) {
            entries.put(key, view);
        }

        private int loads() {
            return loads;
        }

        @Override
        public AuthorizationEvaluator.AuthorizationView get(String key) {
            return entries.computeIfAbsent(key, missing -> {
                loads++;
                return loader.apply(missing);
            });
        }

        @Override
        public void evict(String key) {
            entries.remove(key);
        }

        @Override
        public void evictAll() {
            entries.clear();
        }

        @Override
        public String name() {
            return "test-authorization";
        }
    }
}
