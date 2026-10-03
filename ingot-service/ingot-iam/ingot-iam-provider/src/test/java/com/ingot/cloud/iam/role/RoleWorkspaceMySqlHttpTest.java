package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.assignment.*;
import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.delegation.DelegationAdmission;
import com.ingot.cloud.iam.evaluation.*;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.cloud.iam.support.*;
import com.ingot.cloud.iam.web.v1.platform.PlatformRoleCommandAPI;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>隔离 MySQL 与真实 HTTP 验证角色工作区；可信身份在边界替身中固定，关联 SQL、服务与 Controller 均使用生产实现。</p>
 * <p>显式设置 IAM_MYSQL_HTTP_TEST=true 执行；仅使用已有镜像，结束自动删除测试容器。</p>
 * @author jy
 * @since 1.0.0
 */
@EnabledIfEnvironmentVariable(named="IAM_MYSQL_HTTP_TEST", matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleWorkspaceMySqlHttpTest {
    private String container;
    private AnnotationConfigServletWebServerApplicationContext web;
    private JdbcTemplate jdbc;
    private AuthorizationCandidateMapper candidates;
    private ResourceAccess resources;
    private IamAccess access;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final ActiveIdentity actor = new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "9", "99"), "0", "0", "0");

    @BeforeAll
    void start() throws Exception {
        container = command("docker", "run", "--detach", "--rm", "--pull=never", "--name", "iam-role-workspace-" + UUID.randomUUID(),
                "-p", "127.0.0.1::3306", "-e", "MYSQL_ALLOW_EMPTY_PASSWORD=yes", "-e", "MYSQL_DATABASE=fixture", "-e", "MYSQL_USER=fixture", "-e", "MYSQL_PASSWORD=fixture", "mysql:8.4");
        String address = command("docker", "port", container, "3306/tcp").trim();
        DataSource source = new DriverManagerDataSource("jdbc:mysql://" + address + "/fixture?connectionTimeZone=UTC&allowPublicKeyRetrieval=true&useSSL=false", "fixture", "fixture");
        jdbc = new JdbcTemplate(source);
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (true) {
            try { jdbc.execute("SELECT 1"); break; }
            catch (Exception exception) { if (System.nanoTime() >= deadline) throw exception; Thread.sleep(500); }
        }
        fixture();
        candidates = IamMybatisTestAccess.mapper(source, AuthorizationCandidateMapper.class);
        access = mock(IamAccess.class);
        resources = mock(ResourceAccess.class);
        var roles = mock(RoleService.class);
        when(access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ROLE_READ)).thenReturn(actor);
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ)).thenReturn(new IamAdmission(actor, true));
        when(access.capabilities(eq(actor), anyCollection())).thenAnswer(invocation -> {
            Collection<IamAction> actions = invocation.getArgument(1);
            var admissions = new EnumMap<IamAction, IamActionAuthorizer.Admission>(IamAction.class);
            actions.forEach(action -> admissions.put(action, new IamActionAuthorizer.Admission(true)));
            return new IamCapabilities(admissions);
        });
        when(resources.objects(eq(actor.context()), any())).thenReturn(ObjectScope.all());
        var assignments = IamMybatisTestAccess.assignments(source);
        var presenter = new AssignmentService(access, mock(IamAuditWriter.class), new AuthorizationChangeNotifier(event -> { }),
                roles, mock(RoleRepository.class), assignments, mock(DelegationAdmission.class), resources,
                mock(PlatformAuthorizationEditor.class), mock(TenantScopeCandidates.class), new DataSourceTransactionManager(source));
        var workspace = new PlatformRoleWorkspace(access, resources, roles, assignments, presenter,
                IamMybatisTestAccess.mapper(source, RoleWorkspaceMapper.class));
        web = new AnnotationConfigServletWebServerApplicationContext();
        web.registerBean(ServletWebServerFactory.class, () -> new TomcatServletWebServerFactory(0));
        web.registerBean(PlatformRoleCommandAPI.class, () -> new PlatformRoleCommandAPI(roles, workspace));
        web.registerBean(DispatcherServletRegistrationBean.class, () -> new DispatcherServletRegistrationBean(new DispatcherServlet(web), "/"));
        web.register(Mvc.class);
        web.refresh();
    }
    @AfterAll
    void close() throws Exception {
        if (web != null) web.close();
        if (container != null) command("docker", "stop", "--time", "5", container);
    }
    @BeforeEach
    void resetBoundary() {
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ)).thenReturn(new IamAdmission(actor, true));
        when(resources.objects(eq(actor.context()), any())).thenReturn(ObjectScope.all());
    }
    @Test
    void membersDeduplicateDirectAndGroupSourcesAndIncludeOldFixedVersions() throws Exception {
        JsonNode data = get("/v1/platform/roles/400/members?page=1&pageSize=1");
        assertEquals(2, data.path("total").asInt());
        assertEquals(1, data.path("items").size());
        assertEquals("1", data.path("items").get(0).path("id").asText());
        assertEquals(4, data.path("items").get(0).path("sourceCount").asInt());
        assertEquals(List.of(1, 2), json.convertValue(data.path("items").get(0).path("revisionNumbers"), List.class));
        assertEquals("2", get("/v1/platform/roles/400/members?page=2&pageSize=1").path("items").get(0).path("id").asText());
        assertEquals(1, get("/v1/platform/roles/400/members?revisionId=32").path("total").asInt());
        assertEquals(0, get("/v1/platform/roles/400/members?keyword=missing").path("total").asInt());
    }
    @Test
    void groupVisibilityAndDelegatedOwnershipFilterBeforeCountAndPage() throws Exception {
        when(resources.objects(actor.context(), IamAction.PLATFORM_GROUP_READ)).thenReturn(ObjectScope.none());
        JsonNode direct = get("/v1/platform/roles/400/members");
        assertEquals(1, direct.path("total").asInt());
        assertTrue(direct.path("inheritedSourcesRestricted").asBoolean());
        assertEquals(0, get("/v1/platform/roles/400/groups").path("total").asInt());
        when(resources.objects(actor.context(), IamAction.PLATFORM_GROUP_READ)).thenReturn(ObjectScope.all());
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ)).thenReturn(new IamAdmission(actor, false));
        assertEquals(2, get("/v1/platform/roles/400/members").path("total").asInt());
        assertEquals(1, get("/v1/platform/roles/400/members/1/assignments").path("total").asInt());
    }
    @Test
    void sourcesUseProductionPresentationAndGroupExpansionInvalidatesWholeDerivedGrant() throws Exception {
        JsonNode sources = get("/v1/platform/roles/400/members/1/assignments?page=1&pageSize=2");
        assertEquals(4, sources.path("total").asInt());
        assertEquals(2, sources.path("items").size());
        assertEquals("ACTIVE", sources.path("items").get(0).path("record").path("effectiveStatus").asText());
        assertEquals(1, get("/v1/platform/roles/400/groups").path("total").asInt());
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(80,99)");
        try {
            assertEquals(3, get("/v1/platform/roles/400/members/1/assignments").path("total").asInt());
            jdbc.update("UPDATE iam_delegation_grant SET assignment_duration_mode='LIMITED',max_assignment_duration_seconds=86400 WHERE id=60");
            assertEquals(3, get("/v1/platform/roles/400/members/1/assignments").path("total").asInt());
        } finally {
            jdbc.update("DELETE FROM iam_platform_group_member WHERE group_id=80 AND member_id=99");
            jdbc.update("UPDATE iam_delegation_grant SET assignment_duration_mode='UNLIMITED',max_assignment_duration_seconds=NULL WHERE id=60");
        }
    }
    @Test
    void selectedRelationsUseRealMySqlJsonTableAndExcludeMemberBeforePaging() {
        var objects = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, BigInteger.valueOf(99), null,
                null, "application", "%", List.of(), null, 0, 20, false, null, null, BigInteger.valueOf(60), BigInteger.valueOf(331));
        assertEquals(1, candidates.count(objects));
        assertEquals(BigInteger.valueOf(100), candidates.page(objects).getFirst().id());
        var members = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.MEMBER, BigInteger.valueOf(99), null,
                null, null, "%", List.of(), null, 0, 1, false, null, BigInteger.ONE, BigInteger.valueOf(60), null);
        assertEquals(1, candidates.count(members));
        assertEquals(BigInteger.TWO, candidates.page(members).getFirst().id());
    }
    private JsonNode get(String path) throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + web.getWebServer().getPort() + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).path("data");
    }
    private static String command(String... arguments) throws Exception {
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
        return output.trim();
    }
    private void fixture() {
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY,display_name VARCHAR(100),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_platform_group(id BIGINT PRIMARY KEY,name VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT,member_id BIGINT,PRIMARY KEY(group_id,member_id))");
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY,name VARCHAR(100),domain VARCHAR(16),tenant_id BIGINT,enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY,role_id BIGINT,kind VARCHAR(32),revision BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY,domain VARCHAR(16),tenant_id BIGINT,platform_administrator_id BIGINT,status VARCHAR(16),valid_from DATETIME,valid_until DATETIME,assignment_duration_mode VARCHAR(16),max_assignment_duration_seconds BIGINT,max_assignment_duration_nanos INT)");
        jdbc.execute("CREATE TABLE iam_delegation_role_revision(delegation_id BIGINT,revision_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT,platform_member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_action_ceiling(delegation_id BIGINT,action_id BIGINT,scope_bindings JSON)");
        jdbc.execute("CREATE TABLE iam_application(id BIGINT PRIMARY KEY,name VARCHAR(100),domain VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT,assignment_id BIGINT,change_type VARCHAR(20),actor_member_id BIGINT,occurred_at DATETIME)");
        jdbc.execute("CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY,domain VARCHAR(16),tenant_id BIGINT,subject_type VARCHAR(16),platform_member_id BIGINT,platform_group_id BIGINT,tenant_member_id BIGINT,tenant_group_id BIGINT,revision_id BIGINT,revision_kind VARCHAR(32),scope_bindings JSON,delegation_grant_id BIGINT,valid_from DATETIME,valid_until DATETIME,status VARCHAR(16),source VARCHAR(16),version BIGINT,created_at DATETIME)");
        jdbc.update("INSERT INTO iam_platform_member VALUES(1,'A','ACTIVE'),(2,'B','ACTIVE'),(3,'C','ACTIVE'),(99,'Administrator','ACTIVE')");
        jdbc.update("INSERT INTO iam_platform_group VALUES(80,'Allowed'),(81,'Self group')");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(80,1),(80,2),(81,99),(81,3)");
        jdbc.update("INSERT INTO iam_role_definition VALUES(400,'Role','PLATFORM',NULL,TRUE)");
        jdbc.update("INSERT INTO iam_role_revision VALUES(31,400,'PLATFORM_CUSTOM',1),(32,400,'PLATFORM_CUSTOM',2)");
        jdbc.update("INSERT INTO iam_delegation_grant VALUES(60,'PLATFORM',NULL,99,'ACTIVE',NULL,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 1 DAY),'UNLIMITED',NULL,0)");
        jdbc.update("INSERT INTO iam_delegation_role_revision VALUES(60,31),(60,32)");
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES(60,1),(60,2)");
        jdbc.update("INSERT INTO iam_application VALUES(100,'App','PLATFORM'),(101,'Other','PLATFORM')");
        jdbc.update("INSERT INTO iam_delegation_action_ceiling VALUES(60,331,'{\"objects\":{\"kind\":\"OBJECTS\",\"ids\":[\"100\"]}}')");
        String insert = "INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,platform_group_id,revision_id,revision_kind,scope_bindings,delegation_grant_id,valid_from,valid_until,status,source,version,created_at) VALUES(?, 'PLATFORM', ?, ?, ?, ?, 'PLATFORM_CUSTOM','{}', ?, DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 HOUR), ?, ?, 'MANUAL',0,UTC_TIMESTAMP())";
        jdbc.update(insert, 501,"MEMBER",1,null,31,null,null,"ACTIVE");
        jdbc.update(insert, 502,"MEMBER",1,null,32,null,null,"ACTIVE");
        jdbc.update(insert, 503,"GROUP",null,80,31,null,null,"ACTIVE");
        jdbc.update(insert, 504,"GROUP",null,80,31,60,null,"ACTIVE");
        jdbc.update(insert, 505,"GROUP",null,81,31,60,null,"ACTIVE");
        jdbc.update(insert, 506,"MEMBER",99,null,31,60,null,"ACTIVE");
        jdbc.update(insert, 507,"MEMBER",3,null,31,null,null,"REVOKED");
        jdbc.update(insert, 508,"MEMBER",3,null,31,null,java.sql.Timestamp.valueOf("2001-01-01 00:00:00"),"ACTIVE");
    }
    /**
     * <p>隔离测试只登记目标 Controller，HTTP 仍经真实 Spring MVC 与 JSON 转换。</p>
     * @author jy
     * @since 1.0.0
     */
    @Configuration
    @EnableWebMvc
    static class Mvc implements WebMvcConfigurer {
        /** 使用真实时间及契约类型进行响应序列化。 */
        @Override public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()));
        }
    }
}
