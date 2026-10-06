package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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
import com.ingot.cloud.iam.web.v1.platform.PlatformAssignmentAPI;
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
 * <p>
 * 隔离 MySQL 与真实 HTTP 验证角色工作区；可信身份在边界替身中固定，关联 SQL、服务与 Controller 均使用生产实现。
 * </p>
 * <p>
 * 显式设置 IAM_MYSQL_HTTP_TEST=true 执行；仅使用已有镜像，结束自动删除测试容器。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@EnabledIfEnvironmentVariable(named = "IAM_MYSQL_HTTP_TEST", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleWorkspaceMySqlHttpTest {

    private String container;

    private AnnotationConfigServletWebServerApplicationContext web;

    private JdbcTemplate jdbc;

    private AuthorizationCandidateMapper candidates;

    private ResourceAccess resources;

    private IamAccess access;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private final ActiveIdentity actor = new ActiveIdentity(
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "9", "99"), "0", "0", "0");

    @BeforeAll
    void start() throws Exception {
        container = command("docker", "run", "--detach", "--rm", "--pull=never", "--name",
                "iam-role-workspace-" + UUID.randomUUID(), "-p", "127.0.0.1::3306", "-e",
                "MYSQL_ALLOW_EMPTY_PASSWORD=yes", "-e", "MYSQL_DATABASE=fixture", "-e", "MYSQL_USER=fixture", "-e",
                "MYSQL_PASSWORD=fixture", "mysql:8.4");
        String address = command("docker", "port", container, "3306/tcp").trim();
        DataSource source = new DriverManagerDataSource(
                "jdbc:mysql://" + address + "/fixture?connectionTimeZone=UTC&allowPublicKeyRetrieval=true&useSSL=false",
                "fixture", "fixture");
        jdbc = new JdbcTemplate(source);
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (true) {
            try {
                jdbc.execute("SELECT 1");
                break;
            }
            catch (Exception exception) {
                if (System.nanoTime() >= deadline)
                    throw exception;
                Thread.sleep(500);
            }
        }
        fixture();
        candidates = IamMybatisTestAccess.mapper(source, AuthorizationCandidateMapper.class);
        access = mock(IamAccess.class);
        resources = mock(ResourceAccess.class);
        var roles = mock(RoleService.class);
        var roleStore = mock(RoleRepository.class);
        when(roles.synthesizedGrants(33))
            .thenReturn(List.of(new ActionGrant("331", List.of(new ScopeExpression(ScopeKind.ALL, null, false)))));
        when(roles.synthesizedGrants(31)).thenReturn(List
            .of(new ActionGrant("331", List.of(new ScopeExpression(ScopeKind.OBJECT_SET, "objects.with.dot", false)))));
        when(roles.synthesizedGrants(32))
            .thenReturn(List.of(new ActionGrant("331", List.of(new ScopeExpression(ScopeKind.ALL, null, false)))));
        for (long id : List.of(31L, 32L)) {
            var revision = new com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity();
            revision.setId(BigInteger.valueOf(id));
            revision.setRoleId(BigInteger.valueOf(400));
            revision.setRevision(BigInteger.valueOf(id - 30));
            revision.setKind(RoleKind.PLATFORM_CUSTOM);
            revision.setResourceFieldPermissions("{}");
            when(roleStore.findRevision(id)).thenReturn(revision);
        }
        var parameter = new com.ingot.cloud.iam.persistence.entity.IamRoleParameterEntity();
        parameter.setParameterKey("objects.with.dot");
        parameter.setBindingKind(ScopeBindingKind.OBJECTS);
        when(roleStore.listParameters(31)).thenReturn(List.of(parameter));
        when(access.requireCurrent()).thenReturn(actor);
        when(access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ROLE_READ)).thenReturn(actor);
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ))
            .thenReturn(new IamAdmission(actor, true));
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE))
            .thenReturn(new IamAdmission(actor, true));
        when(access.requireGoverned(eq(AuthorizationDomain.PLATFORM), any())).thenReturn(actor);
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_CREATE))
            .thenReturn(new IamAdmission(actor, true));
        var ids = new AtomicLong(10000);
        when(access.nextId()).thenAnswer(call -> ids.incrementAndGet());
        when(access.capabilities(eq(actor), anyCollection())).thenAnswer(invocation -> {
            Collection<IamAction> actions = invocation.getArgument(1);
            var admissions = new EnumMap<IamAction, IamActionAuthorizer.Admission>(IamAction.class);
            actions.forEach(action -> admissions.put(action, new IamActionAuthorizer.Admission(true)));
            return new IamCapabilities(admissions);
        });
        when(resources.objects(eq(actor.context()), any())).thenReturn(ObjectScope.all());
        var assignments = IamMybatisTestAccess.assignments(source);
        var presenter = new AssignmentService(access, mock(IamAuditWriter.class),
                new AuthorizationChangeNotifier(event -> {
                }), roles, roleStore, assignments, mock(DelegationAdmission.class), resources,
                new PlatformAuthorizationEditor(access, mock(AuthorizationEvaluator.class), assignments,
                        IamMybatisTestAccess.delegations(source), roleStore, roles, candidates,
                        new com.ingot.cloud.iam.extension.BuiltinResourceProviders(candidates).registry(List.of())),
                mock(TenantScopeCandidates.class), new DataSourceTransactionManager(source));
        var workspace = new PlatformRoleWorkspace(access, resources, roles, assignments, presenter,
                IamMybatisTestAccess.mapper(source, RoleWorkspaceMapper.class));
        web = new AnnotationConfigServletWebServerApplicationContext();
        web.registerBean(ServletWebServerFactory.class, () -> new TomcatServletWebServerFactory(0));
        var fieldCompiler = new com.ingot.cloud.iam.extension.ScopeTransportCompiler(mock(DepartmentClosure.class),
                IamMybatisTestAccess.mapper(source, IamMemberDepartmentMapper.class));
        var fieldRegistry = new com.ingot.cloud.iam.extension.BuiltinResourceProviders(candidates).registry(List.of());
        var fieldMetadata = new com.ingot.cloud.iam.extension.ResourceFieldMetadata(
                IamMybatisTestAccess.mapper(source, IamApplicationMapper.class),
                IamMybatisTestAccess.mapper(source, IamResourceMapper.class),
                IamMybatisTestAccess.mapper(source, IamActionMapper.class), fieldRegistry);
        var roleFields = new com.ingot.cloud.iam.extension.RoleFieldPermissionService(fieldMetadata,
                IamMybatisTestAccess.roles(source), fieldCompiler, mock(AuthorizationEvaluator.class));
        var currentIdentity = mock(com.ingot.cloud.iam.identity.CurrentIdentityService.class);
        when(currentIdentity.requireDomain(AuthorizationDomain.PLATFORM)).thenReturn(actor);
        var fieldEvaluator = mock(AuthorizationEvaluator.class);
        var fieldCode = IamAction.PLATFORM_MEMBER_READ.getCode();
        var narrow = new com.ingot.cloud.iam.evaluation.ScopeClause(false, false, false, false, List.of(), false,
                List.of("1"));
        when(fieldEvaluator.evaluateForExecution(eq(actor.context()), anyBoolean()))
            .thenReturn(new AuthorizationEvaluator.AuthorizationView(List.of(fieldCode), List.of(),
                    Map.of(fieldCode,
                            new com.ingot.cloud.iam.evaluation.ResolvedActionScope(
                                    List.of(com.ingot.cloud.iam.evaluation.ScopeClause.universe()))),
                    "31:32", java.time.Instant.now().plusSeconds(1800),
                    List.of(new AuthorizationEvaluator.RoleFieldSource(BigInteger.valueOf(501), null, null, 31,
                            fieldCode, List.of(narrow)),
                            new AuthorizationEvaluator.RoleFieldSource(BigInteger.valueOf(502), null, null, 32,
                                    fieldCode, List.of(com.ingot.cloud.iam.evaluation.ScopeClause.universe())))));
        var localFields = new com.ingot.cloud.iam.extension.LocalResourceAuthorizationClient(currentIdentity,
                fieldEvaluator, fieldRegistry, fieldCompiler, roleFields, candidates);
        web.registerBean(com.ingot.cloud.iam.web.inner.InnerResourceAuthorizationAPI.class,
                () -> new com.ingot.cloud.iam.web.inner.InnerResourceAuthorizationAPI(localFields));
        web.registerBean(PlatformRoleCommandAPI.class, () -> new PlatformRoleCommandAPI(roles, workspace));
        web.registerBean(PlatformAssignmentAPI.class,
                () -> new PlatformAssignmentAPI(presenter, mock(PlatformAuthorizationEditor.class)));
        web.registerBean(DispatcherServletRegistrationBean.class,
                () -> new DispatcherServletRegistrationBean(new DispatcherServlet(web), "/"));
        web.register(Mvc.class);
        web.refresh();
    }

    @AfterAll
    void close() throws Exception {
        if (web != null)
            web.close();
        if (container != null)
            command("docker", "stop", "--time", "5", container);
    }

    @BeforeEach
    void resetBoundary() {
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ))
            .thenReturn(new IamAdmission(actor, true));
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
        assertEquals("2",
                get("/v1/platform/roles/400/members?page=2&pageSize=1").path("items").get(0).path("id").asText());
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
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ))
            .thenReturn(new IamAdmission(actor, false));
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
            jdbc.update(
                    "UPDATE iam_delegation_grant SET assignment_duration_mode='LIMITED',max_assignment_duration_seconds=86400 WHERE id=60");
            assertEquals(3, get("/v1/platform/roles/400/members/1/assignments").path("total").asInt());
        }
        finally {
            jdbc.update("DELETE FROM iam_platform_group_member WHERE group_id=80 AND member_id=99");
            jdbc.update(
                    "UPDATE iam_delegation_grant SET assignment_duration_mode='UNLIMITED',max_assignment_duration_seconds=NULL WHERE id=60");
        }
    }

    @Test
    void selectedRelationsUseRealMySqlJsonTableAndExcludeMemberBeforePaging() {
        var objects = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, BigInteger.valueOf(99),
                null, null, "application", "%", List.of(), null, 0, 20, false, null, null, BigInteger.valueOf(60),
                BigInteger.valueOf(331));
        assertEquals(1, candidates.count(objects));
        assertEquals(BigInteger.valueOf(100), candidates.page(objects).getFirst().id());
        var members = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.MEMBER, BigInteger.valueOf(99),
                null, null, null, "%", List.of(), null, 0, 1, false, null, BigInteger.ONE, BigInteger.valueOf(60),
                null);
        assertEquals(1, candidates.count(members));
        assertEquals(BigInteger.TWO, candidates.page(members).getFirst().id());
    }

    @Test
    void assignmentSelectionUsesStoredParameterAndIntersectsSourceBeforePaging() throws Exception {
        String bindings = "{\"objects.with.dot\":{\"kind\":\"OBJECTS\",\"ids\":[\"100\",\"101\"]},\"other\":{\"kind\":\"OBJECTS\",\"ids\":[\"999\"]}}";
        jdbc.update("UPDATE iam_role_assignment SET scope_bindings=? WHERE id IN (501,504)", bindings);
        try {
            JsonNode roles = get("/v1/platform/assignments/501/selected-candidates?kind=ROLE_REVISION");
            assertEquals(1, roles.path("total").asInt());
            assertEquals("31", roles.path("items").get(0).path("id").asText());
            assertEquals("应用", roles.path("items").get(0).path("actions").get(0).path("resourceName").asText());
            String suffix = "/selected-candidates?kind=OBJECT&parameterKey=objects.with.dot&pageSize=1";
            JsonNode first = get("/v1/platform/assignments/501" + suffix);
            assertEquals(2, first.path("total").asInt());
            assertEquals("100", first.path("items").get(0).path("id").asText());
            assertEquals("101",
                    get("/v1/platform/assignments/501" + suffix + "&page=2").path("items").get(0).path("id").asText());
            JsonNode bounded = get("/v1/platform/assignments/504" + suffix);
            assertEquals(1, bounded.path("total").asInt());
            assertEquals("100", bounded.path("items").get(0).path("id").asText());
            when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ))
                .thenReturn(new IamAdmission(actor, false));
            var denied = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + web.getWebServer().getPort()
                        + "/v1/platform/assignments/501/selected-candidates?kind=ROLE_REVISION"))
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertNotEquals(200, denied.statusCode(), denied.body());
        }
        finally {
            jdbc.update("UPDATE iam_role_assignment SET scope_bindings='{}' WHERE id IN (501,504)");
        }
    }

    @Test
    void customActionCodeDoesNotChangeApplicationObjectsInReplayOrWrite() throws Exception {
        jdbc.update("UPDATE iam_action SET code='aaaa' WHERE id=331");
        jdbc.update("UPDATE iam_role_assignment SET scope_bindings=? WHERE id=501",
                "{\"objects.with.dot\":{\"kind\":\"OBJECTS\",\"ids\":[\"100\"]}}");
        try {
            var objects = get(
                    "/v1/platform/assignments/501/selected-candidates?kind=OBJECT&parameterKey=objects.with.dot");
            assertTrue(objects.path("supported").asBoolean());
            assertEquals(1, objects.path("total").asInt());
            assertEquals("100", objects.path("items").get(0).path("id").asText());
            var input = new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "1"),
                    new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "31"),
                    Map.of("objects.with.dot", new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("100"))), null, null,
                    null);
            var created = post("/v1/platform/assignments", new AssignmentBatchInput(List.of(input)));
            assertEquals(200, created.statusCode(), created.body());
            assertEquals(1,
                    jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE id>10000", Integer.class));
        }
        finally {
            jdbc.update("UPDATE iam_action SET code='iam-platform:application:read' WHERE id=331");
            jdbc.update("UPDATE iam_role_assignment SET scope_bindings='{}' WHERE id=501");
            jdbc.update("DELETE FROM iam_role_assignment WHERE id>10000");
        }
    }

    @Test
    void multiRoleBatchPersistsEverySubjectAndRollsBackWholeInvalidBatch() throws Exception {
        var inputs = new ArrayList<AssignmentInput>();
        for (var subject : List.of(new SubjectRef(SubjectType.MEMBER, "1"), new SubjectRef(SubjectType.GROUP, "80"))) {
            inputs.add(new AssignmentInput(subject, new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "31"),
                    Map.of("objects.with.dot", new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("100"))), null, null,
                    null));
            inputs.add(new AssignmentInput(subject, new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "33"), Map.of(), null,
                    null, null));
        }
        try {
            var created = post("/v1/platform/assignments", new AssignmentBatchInput(inputs));
            assertEquals(200, created.statusCode(), created.body());
            assertEquals(4,
                    jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE id>10000", Integer.class));
            assertEquals(2, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_role_assignment WHERE id>10000 AND subject_type='GROUP'", Integer.class));
            jdbc.update("DELETE FROM iam_role_assignment WHERE id>10000");
            var invalid = new AssignmentBatchInput(
                    List.of(inputs.getFirst(), new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "999"),
                            inputs.get(1).roleRevisionRef(), Map.of(), null, null, null)));
            var preview = post("/v1/platform/assignments/preview", invalid);
            assertEquals(200, preview.statusCode(), preview.body());
            assertFalse(json.readTree(preview.body()).path("data").path("valid").asBoolean());
            var refused = post("/v1/platform/assignments", invalid);
            assertNotEquals(200, refused.statusCode());
            assertEquals(0,
                    jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE id>10000", Integer.class));
        }
        finally {
            jdbc.update("DELETE FROM iam_role_assignment WHERE id>10000");
        }
    }

    @Test
    void upgradeRoleTreeFiltersBeforeCountingAndPaging() {
        var query = new AuthorizationCandidateSql.RoleQuery(BigInteger.valueOf(400), "%", List.of(), null, 0, 1,
                BigInteger.ONE);
        assertEquals(1, candidates.roleCount(query));
        assertEquals(BigInteger.valueOf(32), candidates.rolePage(query).getFirst().id());
        var denied = new AuthorizationCandidateSql.RoleQuery(BigInteger.valueOf(400), "%", List.of(),
                List.of(BigInteger.valueOf(31)), 0, 1, BigInteger.ONE);
        assertEquals(0, candidates.roleCount(denied));
        assertTrue(candidates.rolePage(denied).isEmpty());
    }

    @Test
    void upgradeHttpPreservesRecordAndRejectsBatchVersionConflictAtomically() throws Exception {
        var binding = Map.of("objects.with.dot", new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("100")));
        jdbc.update("UPDATE iam_role_assignment SET scope_bindings=? WHERE id=501", IamJson.object(binding));
        var before = jdbc.queryForMap(
                "SELECT id,platform_member_id,created_at,valid_from,valid_until,delegation_grant_id FROM iam_role_assignment WHERE id=501");
        var target = new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "32");
        try {
            var batch = new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput(target, List.of(
                    new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeItem("501", "0", Map.of()),
                    new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeItem("503", "99", Map.of())));
            assertNotEquals(200, post("/v1/platform/assignments/upgrade", batch).statusCode());
            assertEquals(31L,
                    jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=501", Long.class));
            var input = new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput(target, List
                .of(new com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeItem("501", "0", Map.of())));
            var preview = post("/v1/platform/assignments/upgrade/preview", input);
            assertEquals(200, preview.statusCode(), preview.body());
            assertTrue(json.readTree(preview.body()).path("data").path("valid").asBoolean(), preview.body());
            var saved = post("/v1/platform/assignments/upgrade", input);
            assertEquals(200, saved.statusCode(), saved.body());
            assertEquals(before, jdbc.queryForMap(
                    "SELECT id,platform_member_id,created_at,valid_from,valid_until,delegation_grant_id FROM iam_role_assignment WHERE id=501"));
            assertEquals(32L,
                    jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment WHERE id=501", Long.class));
            assertNotEquals(200, post("/v1/platform/assignments/upgrade", input).statusCode());
        }
        finally {
            jdbc.update("UPDATE iam_role_assignment SET revision_id=31,version=0,scope_bindings='{}' WHERE id=501");
        }
    }

    private HttpResponse<String> put(String path, Object input) throws Exception {
        return HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + web.getWebServer().getPort() + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(input)))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Object input) throws Exception {
        return HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + web.getWebServer().getPort() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(input)))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode get(String path) throws Exception {
        var response = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + web.getWebServer().getPort() + path))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).path("data");
    }

    private static String command(String... arguments) throws Exception {
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
        return output.trim();
    }

    @Test
    void roleFieldsRoundTripViaRealMysqlHttpKeepsContributionScope() throws Exception {
        var key = new com.ingot.framework.commons.model.iam.extension.ResourceKey(AuthorizationDomain.PLATFORM,
                "iam-platform", "member");
        var read = IamAction.PLATFORM_MEMBER_READ.getCode();
        try {
            jdbc.update("UPDATE iam_resource SET field_capabilities=? WHERE id=340",
                    "[{\"key\":\"phone\",\"label\":\"手机号\",\"visibilities\":[\"HIDDEN\",\"MASKED\",\"FULL\"],\"editable\":true,\"filterable\":false,\"sortable\":false}]");
            jdbc.update("UPDATE iam_role_revision SET resource_field_permissions=? WHERE id=31",
                    "{\"340\":{\"phone\":{\"visibility\":\"FULL\",\"editable\":false}}}");
            var response = post("/inner/authorization/v2/evaluate",
                    new com.ingot.framework.commons.model.iam.extension.AuthorizationRequest(key, List.of(read)));
            assertEquals(200, response.statusCode(), response.body());
            var tree = json.readTree(response.body()).path("data");
            assertFalse(tree.has("roleFields"));
            assertFalse(tree.has("fields"));
            var decision = json.treeToValue(tree,
                    com.ingot.framework.commons.model.iam.extension.AuthorizationDecision.class);
            var policy = com.ingot.framework.authorization.FieldPolicyProcessor.forAction(decision, read);
            assertEquals(FieldVisibility.FULL,
                    com.ingot.framework.authorization.FieldPolicyProcessor
                        .access(policy,
                                new com.ingot.framework.commons.model.iam.extension.ScopeTarget("1", "1", null,
                                        List.of()))
                        .get("phone")
                        .visibility());
            assertEquals(FieldVisibility.MASKED,
                    com.ingot.framework.authorization.FieldPolicyProcessor
                        .access(policy,
                                new com.ingot.framework.commons.model.iam.extension.ScopeTarget("3", "3", null,
                                        List.of()))
                        .get("phone")
                        .visibility());
            assertEquals(200,
                    post("/inner/authorization/v2/evaluate", Map.of("resource", key, "actionCodes", List.of(read)))
                        .statusCode());
            jdbc.update("UPDATE iam_role_revision SET resource_field_permissions='{}' WHERE id=31");
            jdbc.update("UPDATE iam_role_revision SET resource_field_permissions='{}' WHERE id=32");
            var empty = post("/inner/authorization/v2/evaluate",
                    new com.ingot.framework.commons.model.iam.extension.AuthorizationRequest(key, List.of(read)));
            assertEquals(200, empty.statusCode(), empty.body());
            var emptyDecision = json.treeToValue(json.readTree(empty.body()).path("data"),
                    com.ingot.framework.commons.model.iam.extension.AuthorizationDecision.class);
            assertEquals(FieldVisibility.HIDDEN,
                    com.ingot.framework.authorization.FieldPolicyProcessor
                        .access(com.ingot.framework.authorization.FieldPolicyProcessor.forAction(emptyDecision, read),
                                new com.ingot.framework.commons.model.iam.extension.ScopeTarget("1", "1", null,
                                        List.of()))
                        .get("phone")
                        .visibility());
        }
        finally {
            jdbc.update("UPDATE iam_role_revision SET resource_field_permissions='{}' WHERE id=31");
            jdbc.update(
                    "UPDATE iam_role_revision SET resource_field_permissions='{\"340\":{\"phone\":{\"visibility\":\"MASKED\",\"editable\":false}}}' WHERE id=32");
            jdbc.update("UPDATE iam_resource SET field_capabilities='[]' WHERE id=340");
        }
    }

    private void fixture() {
        jdbc.execute(
                "CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY,display_name VARCHAR(100),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_platform_group(id BIGINT PRIMARY KEY,name VARCHAR(100))");
        jdbc.execute(
                "CREATE TABLE iam_platform_group_member(group_id BIGINT,member_id BIGINT,PRIMARY KEY(group_id,member_id))");
        jdbc.execute(
                "CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY,name VARCHAR(100),domain VARCHAR(16),tenant_id BIGINT,enabled BOOLEAN)");
        jdbc.execute(
                "CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY,role_id BIGINT,kind VARCHAR(32),revision BIGINT)");
        jdbc.execute(
                "CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY,domain VARCHAR(16),tenant_id BIGINT,platform_administrator_id BIGINT,tenant_administrator_id BIGINT,version BIGINT DEFAULT 0,created_at DATETIME,status VARCHAR(16),valid_from DATETIME,valid_until DATETIME,assignment_duration_mode VARCHAR(16),max_assignment_duration_seconds BIGINT,max_assignment_duration_nanos INT)");
        jdbc.execute(
                "CREATE TABLE iam_delegation_role_revision(delegation_id BIGINT,revision_id BIGINT,revision_kind VARCHAR(32))");
        jdbc.execute(
                "CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT,platform_member_id BIGINT,domain VARCHAR(16) DEFAULT 'PLATFORM',tenant_id BIGINT,tenant_member_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE iam_delegation_action_ceiling(delegation_id BIGINT,action_id BIGINT,scopes JSON,scope_bindings JSON)");
        jdbc.execute(
                "CREATE TABLE iam_application(id BIGINT PRIMARY KEY,name VARCHAR(100),domain VARCHAR(16),enabled BOOLEAN DEFAULT TRUE,code VARCHAR(100))");
        jdbc.execute(
                "CREATE TABLE iam_authorization_audit(id BIGINT,assignment_id BIGINT,change_type VARCHAR(20),actor_member_id BIGINT,occurred_at DATETIME)");
        jdbc.execute(
                "CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY,domain VARCHAR(16),tenant_id BIGINT,subject_type VARCHAR(16),platform_member_id BIGINT,platform_group_id BIGINT,tenant_member_id BIGINT,tenant_group_id BIGINT,revision_id BIGINT,revision_kind VARCHAR(32),scope_bindings JSON,delegation_grant_id BIGINT,valid_from DATETIME,valid_until DATETIME,status VARCHAR(16),source VARCHAR(16),version BIGINT DEFAULT 0,created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        jdbc.update(
                "INSERT INTO iam_platform_member VALUES(1,'A','ACTIVE'),(2,'B','ACTIVE'),(3,'C','ACTIVE'),(99,'Administrator','ACTIVE')");
        jdbc.update("INSERT INTO iam_platform_group VALUES(80,'Allowed'),(81,'Self group')");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(80,1),(80,2),(81,99),(81,3)");
        jdbc.update(
                "INSERT INTO iam_role_definition VALUES(400,'Role','PLATFORM',NULL,TRUE),(401,'Other role','PLATFORM',NULL,TRUE)");
        jdbc.update(
                "INSERT INTO iam_role_revision VALUES(31,400,'PLATFORM_CUSTOM',1),(32,400,'PLATFORM_CUSTOM',2),(33,401,'PLATFORM_CUSTOM',1)");
        jdbc.update(
                "INSERT INTO iam_delegation_grant(id,domain,tenant_id,platform_administrator_id,status,valid_from,valid_until,assignment_duration_mode,max_assignment_duration_seconds,max_assignment_duration_nanos) VALUES(60,'PLATFORM',NULL,99,'ACTIVE',NULL,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 1 DAY),'UNLIMITED',NULL,0)");
        jdbc.update(
                "INSERT INTO iam_delegation_role_revision VALUES(60,31,'PLATFORM_CUSTOM'),(60,32,'PLATFORM_CUSTOM')");
        jdbc.update(
                "INSERT INTO iam_delegation_recipient_member(delegation_id,platform_member_id) VALUES(60,1),(60,2)");
        jdbc.update(
                "INSERT INTO iam_application(id,name,domain,code) VALUES(100,'App','PLATFORM','iam-platform'),(101,'Other','PLATFORM','other')");
        jdbc.update("UPDATE iam_application SET code='iam-platform' WHERE id=100");
        jdbc.execute(
                "CREATE TABLE iam_resource(id BIGINT PRIMARY KEY,application_id BIGINT,name VARCHAR(100),enabled BOOLEAN,scope_capabilities JSON,code VARCHAR(100))");
        jdbc.execute(
                "CREATE TABLE iam_action(id BIGINT PRIMARY KEY,application_id BIGINT,resource_id BIGINT,name VARCHAR(100),code VARCHAR(100),enabled BOOLEAN)");
        jdbc.update("INSERT INTO iam_resource VALUES(330,100,'应用',TRUE,'[\"ALL\",\"OBJECT_SET\"]','application')");
        jdbc.update("INSERT INTO iam_action VALUES(331,100,330,'查看应用','iam-platform:application:read',TRUE)");
        jdbc.update(
                "INSERT INTO iam_delegation_action_ceiling VALUES(60,331,'[{\"kind\":\"OBJECT_SET\",\"parameterKey\":\"objects\"}]','{\"objects\":{\"kind\":\"OBJECTS\",\"ids\":[\"100\"]}}')");
        String insert = "INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,platform_group_id,revision_id,revision_kind,scope_bindings,delegation_grant_id,valid_from,valid_until,status,source,version,created_at) VALUES(?, 'PLATFORM', ?, ?, ?, ?, 'PLATFORM_CUSTOM','{}', ?, DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 HOUR), ?, ?, 'MANUAL',0,UTC_TIMESTAMP())";
        jdbc.update(insert, 501, "MEMBER", 1, null, 31, null, null, "ACTIVE");
        jdbc.update(insert, 502, "MEMBER", 1, null, 32, null, null, "ACTIVE");
        jdbc.update(insert, 503, "GROUP", null, 80, 31, null, null, "ACTIVE");
        jdbc.update(insert, 504, "GROUP", null, 80, 31, 60, null, "ACTIVE");
        jdbc.update(insert, 505, "GROUP", null, 81, 31, 60, null, "ACTIVE");
        jdbc.update(insert, 506, "MEMBER", 99, null, 31, 60, null, "ACTIVE");
        jdbc.update(insert, 507, "MEMBER", 3, null, 31, null, null, "REVOKED");
        jdbc.update(insert, 508, "MEMBER", 3, null, 31, null, java.sql.Timestamp.valueOf("2001-01-01 00:00:00"),
                "ACTIVE");
        jdbc.execute(
                "ALTER TABLE iam_application ADD description VARCHAR(256), ADD icon VARCHAR(256), ADD sort_order INT, ADD baseline BOOLEAN DEFAULT FALSE, ADD version BIGINT DEFAULT 0");
        jdbc.execute("ALTER TABLE iam_resource ADD field_capabilities JSON, ADD version BIGINT DEFAULT 0");
        jdbc.execute(
                "ALTER TABLE iam_platform_member ADD account_id BIGINT, ADD avatar VARCHAR(256), ADD version BIGINT DEFAULT 0, ADD created_at DATETIME, ADD updated_at DATETIME");
        jdbc.execute(
                "ALTER TABLE iam_authorization_audit ADD event_id VARCHAR(64), ADD actor_account_id BIGINT, ADD domain VARCHAR(16), ADD tenant_id BIGINT, ADD target_type VARCHAR(64), ADD target_id VARCHAR(128), ADD safe_before JSON, ADD safe_after JSON, ADD revisions JSON, ADD delegation_id BIGINT, ADD trace_id VARCHAR(128)");
        jdbc.update("UPDATE iam_resource SET field_capabilities='[]'");
        jdbc.update(
                "INSERT INTO iam_resource(id,application_id,name,enabled,scope_capabilities,code,field_capabilities) VALUES(340,100,'成员',TRUE,'[\"ALL\",\"SELF\",\"OBJECT_SET\"]','member','[]')");
        jdbc.execute(
                "ALTER TABLE iam_role_revision ADD base_revision_id BIGINT, ADD metadata_overrides JSON, ADD resource_field_permissions JSON NOT NULL DEFAULT (JSON_OBJECT()), ADD published_at DATETIME");
        jdbc.update(
                "UPDATE iam_role_revision SET resource_field_permissions='{\"340\":{\"phone\":{\"visibility\":\"MASKED\",\"editable\":false}}}' WHERE id=32");
        jdbc.update("INSERT INTO iam_action VALUES(341,100,340,'查看成员','iam-platform:member:read',TRUE)");
    }

    /**
     * <p>
     * 隔离测试只登记目标 Controller，HTTP 仍经真实 Spring MVC 与 JSON 转换。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Configuration
    @EnableWebMvc
    @org.springframework.transaction.annotation.EnableTransactionManagement
    static class Mvc implements WebMvcConfigurer {

        /** 使用真实时间及契约类型进行响应序列化。 */
        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()));
        }

    }

}
