package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>生产候选 SQL 的分页、名单、组全员约束与已选回显保持相同边界。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformCandidateSqlTest {
    private static DataSource source;
    private static AuthorizationCandidateMapper mapper;
    @BeforeAll
    static void fixture() {
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY,display_name VARCHAR(100),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_platform_group(id BIGINT PRIMARY KEY,name VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT,member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_application(id BIGINT PRIMARY KEY,name VARCHAR(100),domain VARCHAR(16),enabled BOOLEAN,code VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_resource(id BIGINT PRIMARY KEY,application_id BIGINT,name VARCHAR(100),enabled BOOLEAN,code VARCHAR(100),scope_capabilities VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_action(id BIGINT PRIMARY KEY,application_id BIGINT,resource_id BIGINT,name VARCHAR(100),enabled BOOLEAN,code VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_menu(id BIGINT PRIMARY KEY,application_id BIGINT,parent_id BIGINT,name VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY,name VARCHAR(100),domain VARCHAR(16),tenant_id BIGINT,enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY,role_id BIGINT,kind VARCHAR(32),revision BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY,platform_administrator_id BIGINT)");
        jdbc.update("INSERT INTO iam_delegation_grant VALUES(60,99)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT,platform_member_id BIGINT)");
        jdbc.update("INSERT INTO iam_platform_member VALUES(1,'张三','ACTIVE'),(2,'李四','ACTIVE'),(3,'王五','ACTIVE')");
        jdbc.update("INSERT INTO iam_platform_group VALUES(10,'允许组'),(11,'越界组'),(12,'空组')");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(10,1),(10,2),(11,1),(11,3)");
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES(60,1),(60,2)");
        jdbc.update("INSERT INTO iam_application VALUES(100,'平台应用','PLATFORM',TRUE,'iam-platform'),(200,'租户应用','TENANT',TRUE,'iam-tenant')");
        jdbc.update("INSERT INTO iam_resource VALUES(210,100,'成员管理',TRUE,'member','[]'),(211,100,'用户组管理',TRUE,'group','[]'),(212,200,'租户资源',TRUE,'member','[]')");
        jdbc.update("INSERT INTO iam_action VALUES(300,100,210,'创建',TRUE,'aaaa'),(301,100,211,'创建',TRUE,'iam-platform:application:create'),(302,200,212,'创建',TRUE,'iam-platform:member:create'),(303,100,212,'不一致关联',TRUE,'iam-platform:member:create')");
        jdbc.update("INSERT INTO iam_menu VALUES(700,100,NULL,'应用配置'),(701,100,700,'资源菜单'),"
                + "(702,200,NULL,'租户菜单')");
        jdbc.update("INSERT INTO iam_role_definition VALUES(400,'平台治理','PLATFORM',NULL,TRUE),(401,'平台测试','PLATFORM',NULL,TRUE),(402,'租户角色','TENANT',1,TRUE),(403,'停用角色','PLATFORM',NULL,FALSE)");
        jdbc.update("INSERT INTO iam_role_revision VALUES(500,400,'PLATFORM_CUSTOM',1),(501,400,'PLATFORM_CUSTOM',2),(502,401,'SYSTEM',1),(503,402,'TENANT_CUSTOM',1),(504,403,'PLATFORM_CUSTOM',1)");
        mapper = IamMybatisTestAccess.mapper(source, AuthorizationCandidateMapper.class);
    }
    @Test
    void actionMetadataUsesAssociatedCodesAndRejectsCrossApplicationResource() {
        var rows = mapper.actions(List.of(BigInteger.valueOf(300), BigInteger.valueOf(301),
                BigInteger.valueOf(302), BigInteger.valueOf(303)));
        assertEquals(2, rows.size());
        assertEquals("iam-platform", rows.getFirst().applicationCode());
        assertEquals("member", rows.getFirst().resourceCode());
        assertEquals("aaaa", rows.getFirst().code());
        assertEquals("group", rows.get(1).resourceCode());
        assertEquals("iam-platform:application:create", rows.get(1).code());
    }

    @Test
    void groupCandidateMustBeNonEmptyAndEveryMemberMustBeReached() {
        var query = query(AuthorizationCandidateKind.GROUP, null, List.of(), 0, 20);
        assertEquals(1, mapper.count(query));
        assertEquals(BigInteger.TEN, mapper.page(query).getFirst().id());
    }
    @Test
    void selectedMemberReplayCannotEscapeAllowedIdsAndCountMatchesPage() {
        var query = query(AuthorizationCandidateKind.MEMBER, List.of(BigInteger.ONE, BigInteger.TWO),
                List.of(BigInteger.TWO, BigInteger.valueOf(3)), 0, 20);
        assertEquals(1, mapper.count(query));
        assertEquals(BigInteger.TWO, mapper.page(query).getFirst().id());
    }
    @Test
    void pagesContinueWithoutTruncatingAtOnePage() {
        var first = query(AuthorizationCandidateKind.MEMBER, null, List.of(), 0, 2);
        var second = query(AuthorizationCandidateKind.MEMBER, null, List.of(), 2, 2);
        assertEquals(3, mapper.count(first)); assertEquals(2, mapper.page(first).size());
        assertEquals(BigInteger.valueOf(3), mapper.page(second).getFirst().id());
    }

    @Test
    void platformObjectCandidatesDoNotIncludeTenantApplications() {
        var query = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, BigInteger.ONE,
                null, null, "application", "%", List.of(), null, 0, 20);
        assertEquals(1, mapper.count(query));
        assertEquals(BigInteger.valueOf(100), mapper.page(query).getFirst().id());
    }

    @Test
    void menuTreeUsesPlatformBranchAndAncestorPath() {
        var roots = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, BigInteger.ONE,
                null, null, "menu", "%", List.of(), null, 0, 20, true, null);
        assertEquals(1, mapper.count(roots));
        assertEquals(BigInteger.valueOf(700), mapper.page(roots).getFirst().id());
        var children = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, BigInteger.ONE,
                null, null, "menu", "%", List.of(), null, 0, 20, true, BigInteger.valueOf(700));
        assertEquals(BigInteger.valueOf(701), mapper.page(children).getFirst().id());
        assertEquals("应用配置 / 资源菜单", mapper.menuPaths(List.of(BigInteger.valueOf(701)))
                .getFirst().ancestorPath());
        assertEquals(java.util.Set.of(BigInteger.valueOf(700), BigInteger.valueOf(701)),
                java.util.Set.copyOf(mapper.menuAncestorIds(List.of(BigInteger.valueOf(701)))));
    }
    @Test
    void actionCandidatesSearchResourceAndActionNamesWithinTheSelectedApplication() {
        var byResource = actionQuery("%用户组管理%", 0, 20);
        assertEquals(1, mapper.count(byResource));
        var groupCreate = mapper.page(byResource).getFirst();
        assertEquals(BigInteger.valueOf(301), groupCreate.id());
        assertEquals("创建", groupCreate.name());
        assertEquals("用户组管理", groupCreate.resourceName());

        var first = actionQuery("%创建%", 0, 1);
        var second = actionQuery("%创建%", 1, 1);
        assertEquals(2, mapper.count(first));
        assertEquals(BigInteger.valueOf(300), mapper.page(first).getFirst().id());
        assertEquals(BigInteger.valueOf(301), mapper.page(second).getFirst().id());
    }

    @Test
    void delegationRoleTreeKeepsPlatformBoundarySearchAndIndependentLayerPaging() {
        var root = new AuthorizationCandidateSql.RoleQuery(null, "%平台%", List.of(), null, 0, 1);
        assertEquals(2, mapper.roleCount(root));
        assertEquals(BigInteger.valueOf(400), mapper.rolePage(root).getFirst().id());
        var next = new AuthorizationCandidateSql.RoleQuery(null, "%平台%", List.of(), null, 1, 1);
        assertEquals(BigInteger.valueOf(401), mapper.rolePage(next).getFirst().id());
        var versions = new AuthorizationCandidateSql.RoleQuery(BigInteger.valueOf(400), "%平台治理%",
                List.of(), null, 0, 1);
        assertEquals(2, mapper.roleCount(versions));
        assertEquals(BigInteger.valueOf(501), mapper.rolePage(versions).getFirst().id());
        var selected = new AuthorizationCandidateSql.RoleQuery(BigInteger.valueOf(400), "%平台治理%",
                List.of(BigInteger.valueOf(500)), null, 0, 20);
        assertEquals(1, mapper.roleCount(selected));
        assertEquals(BigInteger.valueOf(500), mapper.rolePage(selected).getFirst().id());
    }

    private static AuthorizationCandidateSql.Query actionQuery(String keyword, int offset, int size) {
        return new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.ACTION, BigInteger.ONE,
                null, BigInteger.valueOf(100), null, keyword, List.of(), null, offset, size);
    }
    private static AuthorizationCandidateSql.Query query(AuthorizationCandidateKind kind, List<BigInteger> allowed,
            List<BigInteger> ids, int offset, int size) {
        return new AuthorizationCandidateSql.Query(kind, BigInteger.ONE, BigInteger.valueOf(60), null, null,
                "%", ids, allowed, offset, size);
    }
}
