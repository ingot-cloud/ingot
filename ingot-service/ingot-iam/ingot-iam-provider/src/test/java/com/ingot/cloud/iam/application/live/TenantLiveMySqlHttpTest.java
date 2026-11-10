package com.ingot.cloud.iam.application.live;

import com.ingot.cloud.iam.application.group.GroupService;
import com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade;
import com.ingot.cloud.iam.application.authorization.extension.LocalResourceAuthorizationClient;
import com.ingot.cloud.iam.application.organization.MemberFacade;
import com.ingot.cloud.iam.application.organization.MemberProfileCommandService;
import com.ingot.cloud.iam.application.organization.MemberQueryService;
import com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments;
import com.ingot.cloud.iam.application.organization.OrganizationMemberQueryService;
import com.ingot.cloud.iam.infrastructure.persistence.OrganizationMemberRepository;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformGroupAPI;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformMemberCommandAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantMemberCommandAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantGroupAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantWorkbenchAPI;
import com.ingot.cloud.iam.application.tenant.TenantWorkbenchService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;

import com.ingot.cloud.iam.application.authorization.scope.TenantScopeBindingService;
import com.ingot.cloud.iam.application.role.TenantRoleLockService;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionResolver;

import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.PlatformAdministratorGuard;
import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.application.identity.ActiveIdentityService;
import com.ingot.cloud.iam.application.identity.CurrentIdentityService;
import com.ingot.cloud.iam.application.role.RoleGrantValidator;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionService;
import com.ingot.cloud.iam.application.shared.TenantAuthorizationTransaction;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport;
import com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.infrastructure.persistence.SessionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamResourceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantAssignmentAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantDelegationAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantRoleCommandAPI;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.field.ClientFieldPolicyProvider;
import com.ingot.framework.authorization.field.FieldWriteExecutor;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AuthorizationOption;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;

import com.ingot.cloud.iam.application.organization.model.MemberProfileInput;
import com.ingot.cloud.iam.application.organization.model.MemberRecord;
import com.ingot.cloud.iam.application.role.model.CurrentRoleCatalogAction;
import com.ingot.cloud.iam.domain.organization.MemberResources;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.*;
import com.ingot.cloud.iam.configuration.IamReadRequestConfiguration;
import com.ingot.cloud.iam.application.catalog.CatalogFacade;
import com.ingot.cloud.iam.application.catalog.EntitlementResolver;
import com.ingot.cloud.iam.application.catalog.EntitlementService;
import com.ingot.cloud.iam.application.catalog.model.ApplicationPurgeInput;
import com.ingot.cloud.iam.application.shared.model.SensitiveConfirmation;
import com.ingot.cloud.iam.domain.shared.SensitiveConfirmationKind;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformApplicationAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantApplicationAPI;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.application.session.SessionService;
import com.ingot.cloud.iam.interfaces.http.IamErrorHandler;
import com.ingot.cloud.iam.interfaces.http.CurrentSessionAPI;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformSharedRoleCommandAPI;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>隔离MySQL与两个真实HTTP节点验证当前角色、拥有者、受限委派和旧契约退出。</p>
 * <p>测试身份过滤器只存在本夹具；生产控制器、Mapper、事务和准入使用真实实现。</p>
 * @author jy
 * @since 1.0.0
 */
@EnabledIfEnvironmentVariable(named="IAM_TENANT_LIVE_MYSQL_HTTP_TEST",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantLiveMySqlHttpTest {
    private String container;
    private JdbcTemplate jdbc;
    private com.zaxxer.hikari.HikariDataSource pool;
    private AnnotationConfigServletWebServerApplicationContext first,second;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules().registerModule(new com.ingot.framework.commons.jackson.InApiTimeModule()).configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    private final HttpClient http=HttpClient.newHttpClient();
    private final AtomicLong ids=new AtomicLong(10_000_000);
    private final AtomicLong internalRpcCalls=new AtomicLong();
    private final AtomicBoolean expireMemberWritePreview = new AtomicBoolean();
    private static final Map<String,AuthorizationContext> ACTORS=new java.util.concurrent.ConcurrentHashMap<>(Map.of(
        "platform",new AuthorizationContext(AuthorizationDomain.PLATFORM,null,"1","1001"),
        "owner-a",new AuthorizationContext(AuthorizationDomain.TENANT,"10","1","101"),
        "owner-b",new AuthorizationContext(AuthorizationDomain.TENANT,"20","1","201"),
        "member-a",new AuthorizationContext(AuthorizationDomain.TENANT,"10","2","102"),
        "member-b",new AuthorizationContext(AuthorizationDomain.TENANT,"20","2","202"),
        "delegate-a",new AuthorizationContext(AuthorizationDomain.TENANT,"10","3","103"),
        "recipient-a",new AuthorizationContext(AuthorizationDomain.TENANT,"10","4","104")));

    @BeforeAll
    void start() throws Exception {
        container=command("docker","run","--detach","--rm","--pull=never","--name","iam-tenant-live-"+UUID.randomUUID(),"-p","127.0.0.1::3306","-e","MYSQL_ALLOW_EMPTY_PASSWORD=yes","-e","MYSQL_ROOT_HOST=%","-e","MYSQL_DATABASE=fixture","mysql:8.4");
        var address=command("docker","port",container,"3306/tcp");
        var jdbcOptions="?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&useSSL=false";
        var source=new DriverManagerDataSource("jdbc:mysql://"+address+"/fixture"+jdbcOptions,"root","");
        jdbc=new JdbcTemplate(source);long deadline=System.nanoTime()+Duration.ofSeconds(60).toNanos();
        while(true){try{jdbc.execute("SELECT 1");break;}catch(Exception e){if(System.nanoTime()>deadline)throw e;Thread.sleep(500);}}
        Path root=Path.of("").toAbsolutePath();while(!Files.exists(root.resolve("databases/iam/manifest.json")))root=root.getParent();
        new ResourceDatabasePopulator(new FileSystemResource(root.resolve("databases/ingot_iam.sql"))).execute(source);
        pool=new com.zaxxer.hikari.HikariDataSource();pool.setJdbcUrl("jdbc:mysql://"+address+"/ingot_iam"+jdbcOptions);pool.setUsername("root");pool.setPassword("");pool.setMaximumPoolSize(64);pool.setMinimumIdle(8);pool.setConnectionTimeout(5000);jdbc=new JdbcTemplate(pool);
        jdbc.update("INSERT INTO iam_account(id,username,password_hash) VALUES(1,'fixture-1','not-a-credential'),(2,'fixture-2','not-a-credential'),(3,'fixture-3','not-a-credential'),(4,'fixture-4','not-a-credential')");
        jdbc.update("INSERT INTO iam_platform_member(id,account_id,display_name) VALUES(1001,1,'平台')");
        // 平台超管查询使用秒级时钟，夹具种子在首个请求前应已生效。
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,revision_id,revision_kind,scope_bindings,valid_from,source) SELECT 900,'PLATFORM','MEMBER',1001,r.id,'SYSTEM','{}',UTC_TIMESTAMP(6)-INTERVAL 1 SECOND,'INITIALIZATION' FROM iam_role_revision r JOIN iam_role_definition d ON d.id=r.role_id WHERE d.domain='PLATFORM' AND d.kind='SYSTEM' ORDER BY r.revision DESC LIMIT 1");
        jdbc.update("INSERT INTO iam_tenant(id,name) VALUES(10,'组织A'),(20,'组织B')");
        jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name) VALUES(101,10,1,'拥有者A'),(102,10,2,'成员A'),(103,10,3,'管理员A'),(104,10,4,'接收人A'),(201,20,1,'拥有者B'),(202,20,2,'成员B')");
        jdbc.update("UPDATE iam_tenant SET owner_member_id=CASE id WHEN 10 THEN 101 ELSE 201 END");
        jdbc.update("INSERT INTO iam_department(id,tenant_id,parent_id,name) VALUES(11,10,NULL,'研发'),(12,10,11,'研发子部门'),(13,10,NULL,'营销')");
        jdbc.update("INSERT INTO iam_member_department(tenant_id,member_id,department_id) VALUES(10,104,12)");
        jdbc.update("INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,valid_from,source) SELECT 600+tenant.id,tenant.id,app.id,TRUE,'2000-01-01','INITIALIZATION' FROM iam_tenant tenant JOIN iam_application app ON app.code='iam-tenant'");
        jdbc.update("INSERT INTO iam_field_policy(tenant_id,resource_id,default_revision_id) SELECT t.id,d.resource_id,d.id FROM iam_tenant t JOIN iam_default_policy_revision d ON d.kind='FIELD' WHERE d.revision=(SELECT MAX(revision) FROM iam_default_policy_revision WHERE kind='FIELD' AND resource_id=d.resource_id)");
        jdbc.update("INSERT INTO iam_app_audience(tenant_id,application_id,audience_kind) SELECT tenant.id,app.id,'ALL' FROM iam_tenant tenant JOIN iam_application app ON app.code='iam-tenant'");
        first = node(pool);
        second = node(pool);
    }
    private AnnotationConfigServletWebServerApplicationContext node(DataSource source) {
        var web=new AnnotationConfigServletWebServerApplicationContext();
        var evaluator=IamMybatisTestAccess.evaluator(source);
        var identities=new CurrentIdentityService(new ActiveIdentityService(IamMybatisTestAccess.identity(source)));
        var changes=new AuthorizationChangeNotifier(event->{ });
        var access=new IamAccess(identities,evaluator,ids::incrementAndGet,new PlatformAdministratorGuard(IamMybatisTestAccess.assignments(source),changes));
        var live=IamMybatisTestAccess.mapper(source,TenantAdministrationMapper.class);
        var resources=IamMybatisTestAccess.resourceAccess(source);
        var transaction=new TenantAuthorizationTransaction(new DataSourceTransactionManager(source));
        var roleService=new TenantRoleDefinitionService(access,resources,live,IamMybatisTestAccess.currentRoles(source),new RoleGrantValidator(IamMybatisTestAccess.roles(source)),transaction,IamMybatisTestAccess.audits(source),changes,IamMybatisTestAccess.roleFields(source),IamMybatisTestAccess.fieldMetadata(source));
        var assignmentService=com.ingot.cloud.iam.fixture.TenantAssignmentFixture.create(access,live,IamMybatisTestAccess.mapper(source,com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper.class),IamMybatisTestAccess.currentRoles(source),roleService,resources,transaction,IamMybatisTestAccess.audits(source),changes,IamMybatisTestAccess.mapper(source,TenantScopeCandidateMapper.class),new DepartmentClosure(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class)),new com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments(access,resources,IamMybatisTestAccess.memberQueries(source),new DepartmentQueryRepository(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class),IamMybatisTestAccess.mapper(source,IamMemberDepartmentMapper.class),IamMybatisTestAccess.mapper(source,IamTenantMemberMapper.class))));
        var delegationService=com.ingot.cloud.iam.fixture.TenantDelegationFacadeFixture.create(access,live,IamMybatisTestAccess.currentRoles(source),new TenantRoleDefinitionResolver(live,IamMybatisTestAccess.currentRoles(source)),new TenantRoleLockService(live),new TenantScopeBindingService(live,IamMybatisTestAccess.mapper(source,TenantScopeCandidateMapper.class),new DepartmentClosure(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class))),resources,transaction,IamMybatisTestAccess.audits(source),changes);
        var sessionStore=new SessionRepository(IamMybatisTestAccess.mapper(source,IamPlatformMemberMapper.class),IamMybatisTestAccess.mapper(source,IamTenantMemberMapper.class),IamMybatisTestAccess.mapper(source,IamApplicationMapper.class),IamMybatisTestAccess.mapper(source,IamMenuMapper.class),IamMybatisTestAccess.entitlements(source));
        var sessions=new SessionService(access,evaluator,sessionStore,live);
        var local = spy(registerResourceClients(web, source, identities, evaluator));
        doAnswer(invocation -> {
            AuthorizationDecision decision = (AuthorizationDecision) invocation.callRealMethod();
            var request = invocation.getArgument(0, AuthorizationRequest.class);
            if (expireMemberWritePreview.get() && request.resource().equals(MemberResources.TENANT_MEMBER)
                    && request.actionCodes().equals(List.of(IamAction.VALUE_TENANT_MEMBER_UPDATE))) {
                return new AuthorizationDecision(decision.resource(), decision.context(), decision.actions(),
                        decision.version(), Instant.EPOCH);
            }
            return decision;
        }).when(local).preview(any(AuthorizationRequest.class));
        var authorization = new AuthorizationAccess(local);
        web.registerBean(ServletWebServerFactory.class,()->new TomcatServletWebServerFactory(0));
        web.registerBean(TenantRoleCommandAPI.class,()->new TenantRoleCommandAPI(roleService,assignmentService,identities));
        web.registerBean(PlatformSharedRoleCommandAPI.class,()->new PlatformSharedRoleCommandAPI(roleService,identities));
        web.registerBean(TenantAssignmentAPI.class,()->new TenantAssignmentAPI(assignmentService));
        web.registerBean(TenantDelegationAPI.class,()->new TenantDelegationAPI(delegationService));
        web.registerBean(FixtureCatalog.class,()->new FixtureCatalog(access,live));
        var transactions = new DataSourceTransactionManager(source);
        var catalog = IamMybatisTestAccess.catalogService(access, source, transactions);
        var catalogRepository = IamMybatisTestAccess.catalogs(source);
        var entitlements = new EntitlementService(access, IamMybatisTestAccess.audits(source), changes,
                catalogRepository, IamMybatisTestAccess.entitlements(source), IamMybatisTestAccess.tenants(source),
                new EntitlementResolver(IamMybatisTestAccess.catalog(source), catalogRepository), transactions);
        var groups = IamMybatisTestAccess.groups(source);
        var members = IamMybatisTestAccess.memberQueries(source);
        var organizationMembers = new OrganizationMemberRepository(IamMybatisTestAccess.mapper(source, IamTenantMemberMapper.class));
        var departments = new DepartmentQueryRepository(IamMybatisTestAccess.mapper(source, IamDepartmentMapper.class),
                IamMybatisTestAccess.mapper(source, IamMemberDepartmentMapper.class),
                IamMybatisTestAccess.mapper(source, IamTenantMemberMapper.class));
        var closure = new DepartmentClosure(IamMybatisTestAccess.mapper(source, IamDepartmentMapper.class));
        var groupService = new GroupService(access, IamMybatisTestAccess.audits(source), changes, groups,
                IamMybatisTestAccess.recipients(source), closure, resources, organizationMembers, transactions);
        var manifests = com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.manifests();
        var registry = new com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders(
                IamMybatisTestAccess.mapper(source, AuthorizationCandidateMapper.class)).registry(List.of());
        var metadata = new com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata(
                IamMybatisTestAccess.mapper(source, IamApplicationMapper.class),
                IamMybatisTestAccess.mapper(source, IamResourceMapper.class),
                IamMybatisTestAccess.mapper(source, IamActionMapper.class), registry, manifests,
                com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache());
        var fields = Boolean.parseBoolean(System.getenv("IAM_TENANT_CONTEXT_BENCHMARK"))
                ? com.ingot.cloud.iam.fixture.FieldAccessEvaluatorFixture.create(
                        IamMybatisTestAccess.policies(source), IamMybatisTestAccess.mapper(source, IamMemberDepartmentMapper.class),
                        closure, com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache(),
                        com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.projection(), metadata,
                        com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache())
                : IamMybatisTestAccess.fields(source);
        var platformFields = new com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService(
                metadata, IamMybatisTestAccess.roles(source),
                new com.ingot.cloud.iam.infrastructure.persistence.AuthorizationScopeTransportCompiler(closure,
                        IamMybatisTestAccess.mapper(source, IamMemberDepartmentMapper.class)),
                evaluator, manifests, com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache());
        var platformAssignments = mock(PlatformAssignmentFacade.class);
        var memberQueries = new MemberQueryService(platformFields, access, authorization, resources,
                new com.ingot.cloud.iam.application.authorization.evaluation.ObjectCapabilities(evaluator, resources),
                fields, members, groups, platformAssignments);
        var writes = new FieldWriteExecutor(new ClientFieldPolicyProvider(authorization, registry));
        var memberCommands = new MemberProfileCommandService(platformFields, FieldTestSupport.projection(), writes,
                access, resources, IamMybatisTestAccess.audits(source), members, platformAssignments,
                groupService, transactions, IamMybatisTestAccess.images(), memberQueries);
        var organizationQueries = new OrganizationMemberQueryService(access, resources, organizationMembers,
                new OrganizationMemberDepartments(access, resources, members, departments), fields, groups,
                closure, departments, members);
        var memberFacade = new MemberFacade(memberQueries, memberCommands);
        web.registerBean(PlatformGroupAPI.class, () -> new PlatformGroupAPI(groupService, memberFacade));
        web.registerBean(PlatformMemberCommandAPI.class, () -> new PlatformMemberCommandAPI(null, memberFacade, null));
        web.registerBean(TenantMemberCommandAPI.class, () -> new TenantMemberCommandAPI(
                null, memberFacade, null, null, null, null, organizationQueries));
        web.registerBean(TenantGroupAPI.class, () -> new TenantGroupAPI(groupService, organizationQueries));
        web.registerBean(TenantWorkbenchAPI.class, () -> new TenantWorkbenchAPI(
                new TenantWorkbenchService(sessions, resources, IamMybatisTestAccess.tenants(source),
                        members, departments, groups)));
        web.registerBean(CatalogFacade.class, () -> catalog);
        web.registerBean(PlatformApplicationAPI.class, () -> new PlatformApplicationAPI(catalog));
        web.registerBean(TenantApplicationAPI.class, () -> new TenantApplicationAPI(entitlements));
        web.registerBean(CurrentSessionAPI.class,()->new CurrentSessionAPI(sessions,org.mockito.Mockito.mock(com.ingot.cloud.iam.application.account.CurrentAccountService.class)));
        web.registerBean(IamErrorHandler.class,IamErrorHandler::new);
        web.registerBean(com.ingot.cloud.iam.interfaces.http.TenantCurrentContractErrorHandler.class,com.ingot.cloud.iam.interfaces.http.TenantCurrentContractErrorHandler::new);
        web.registerBean(DispatcherServletRegistrationBean.class,()->new DispatcherServletRegistrationBean(new DispatcherServlet(web),"/"));
        web.registerBean(FilterRegistrationBean.class,()->new FilterRegistrationBean<>(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,jakarta.servlet.FilterChain chain) throws java.io.IOException,jakarta.servlet.ServletException {
                var actor=ACTORS.get(request.getHeader("X-Fixture-Actor"));
                if(actor==null){response.setStatus(401);return;}
                authenticateFixture(actor);
                try{chain.doFilter(request,response);}finally{SecurityContextHolder.clearContext();}
            }}));
        web.register(Mvc.class,IamReadRequestConfiguration.class);web.refresh();return web;
    }
    private static void authenticateFixture(AuthorizationContext actor) {
        var user = InUser.stateless(Long.parseLong(actor.accountId()),
                actor.tenantId() == null ? null : Long.parseLong(actor.tenantId()), "web", "standard",
                UserTypeEnum.ADMIN.getValue(), "fixture", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(actor).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }

    @AfterAll
    void stop() throws Exception {
        if (first != null) first.close();
        if (second != null) second.close();
        if (pool != null) pool.close();
        if (container != null) command("docker","stop","--timeout","10",container);
    }

    @Test
    @Order(1)
    void currentRolesAcrossTenantsAndRestrictedDelegationUseNextRequestFacts() throws Exception {
        String read=action(IamAction.VALUE_TENANT_MEMBER_READ),update=action(IamAction.VALUE_TENANT_MEMBER_UPDATE),department=action(IamAction.VALUE_TENANT_DEPARTMENT_READ);
        var create=request(first,"platform","POST","/v1/platform/shared-roles",Map.of("code","fixture-shared","name","共享角色","definition",definition(read)));
        assertEquals(200,create.statusCode(),create.body());String role=data(create).path("id").asText();
        assign("owner-a","102",role,null);assign("owner-b","202",role,null);
        assertTrue(codes(second,"member-a").contains(IamAction.VALUE_TENANT_MEMBER_READ));
        assertTrue(codes(second,"member-b").contains(IamAction.VALUE_TENANT_MEMBER_READ));
        String lock=data(request(first,"platform","GET","/v1/platform/shared-roles/"+role,null)).path("version").asText();
        assertEquals(200,request(first,"platform","PUT","/v1/platform/shared-roles/"+role+"/grants",Map.of("expectedVersion",lock,"definition",definition(update))).statusCode());
        assertFalse(codes(second,"member-a").contains(IamAction.VALUE_TENANT_MEMBER_READ));assertTrue(codes(second,"member-a").contains(IamAction.VALUE_TENANT_MEMBER_UPDATE));assertTrue(codes(second,"member-b").contains(IamAction.VALUE_TENANT_MEMBER_UPDATE));
        assertEquals(403,request(first,"owner-a","PATCH","/v1/tenant/roles/"+role,Map.of("expectedVersion","2","status","DISABLED")).statusCode());
        assertEquals(410,request(first,"owner-a","GET","/v1/tenant/roles/"+role+"/revisions",null).statusCode());
        assertEquals(400,request(first,"owner-a","POST","/v1/tenant/roles",Map.of("code","bad","name","旧参数","definition",definition(read),"baseRevisionId","1")).statusCode());
        var resource=jdbc.queryForObject("SELECT resource_id FROM iam_action WHERE id=?",Long.class,new BigInteger(update));
        var delegation=request(first,"owner-a","POST","/v1/tenant/delegations",Map.of("administratorMemberId","103","allowedRoleIds",List.of(role),"recipientSelection",Map.of("members",List.of("104"),"departments",List.of()),"resourceScopeCeilings",List.of(Map.of("resourceId",resource.toString(),"scopes",List.of(Map.of("kind","MANAGED_DEPARTMENTS","parameterKey","dept","includeDescendants",true)),"scopeBindings",Map.of("dept",Map.of("kind","DEPARTMENTS","ids",List.of("11"))))),"maxAssignmentDuration","P7D"));
        assertEquals(200,delegation.statusCode(),delegation.body());String source=data(delegation).path("id").asText();
        assertEquals(403,request(first,"delegate-a","GET","/v1/tenant/roles",null).statusCode());
        var candidates=request(first,"delegate-a","GET","/v1/tenant/assignments/options?kind=MEMBER&delegationGrantId="+source,null);
        assertEquals(200,candidates.statusCode(),candidates.body());assertEquals(1,data(candidates).path("items").size());assertEquals("104",data(candidates).path("items").get(0).path("id").asText());
        assign("delegate-a","104",role,source);assertTrue(codes(second,"recipient-a").contains(IamAction.VALUE_TENANT_MEMBER_UPDATE));
        var bounded=request(second,"recipient-a","GET","/fixture/consumer?actionCode="+IamAction.VALUE_TENANT_MEMBER_UPDATE,null);assertEquals(200,bounded.statusCode(),bounded.body());
        var scope=data(bounded).path("actions").path(IamAction.VALUE_TENANT_MEMBER_UPDATE).path("scope").get(0);
        assertFalse(scope.path("all").asBoolean());assertTrue(scope.path("departmentSets").toString().contains("11"));assertTrue(scope.path("departmentSets").toString().contains("12"));assertFalse(scope.path("departmentSets").toString().contains("13"));
        lock=data(request(first,"platform","GET","/v1/platform/shared-roles/"+role,null)).path("version").asText();
        var next=definition(update,department);assertEquals(200,request(first,"platform","PUT","/v1/platform/shared-roles/"+role+"/grants",Map.of("expectedVersion",lock,"definition",next)).statusCode());
        assertTrue(codes(second,"member-a").contains(IamAction.VALUE_TENANT_DEPARTMENT_READ));assertFalse(codes(second,"recipient-a").contains(IamAction.VALUE_TENANT_DEPARTMENT_READ));
        var mine=data(request(first,"delegate-a","GET","/v1/tenant/delegations/mine",null));assertEquals("MISSING_RESOURCE_CEILING",mine.get(0).path("scopeIssues").get(0).path("reason").asText());
        assertEquals(200,request(first,"owner-a","DELETE","/v1/tenant/delegations/"+source,null).statusCode());assertFalse(codes(second,"recipient-a").contains(IamAction.VALUE_TENANT_MEMBER_UPDATE));
        assertTrue(data(request(first,"owner-a","GET","/v1/me/bootstrap",null)).path("tenantOwner").asBoolean());
        jdbc.update("UPDATE iam_app_audience SET audience_kind='SELECTED' WHERE tenant_id=10");assertTrue(codes(second,"owner-a").contains(IamAction.VALUE_TENANT_MEMBER_READ));
        var privateRole=request(first,"owner-a","POST","/v1/tenant/roles",Map.of("code","private-a","name","组织私有","definition",definition(read)));
        assertEquals(200,privateRole.statusCode(),privateRole.body());assertEquals(404,request(second,"owner-b","GET","/v1/tenant/roles/"+data(privateRole).path("id").asText(),null).statusCode());
    }
    @Test
    @Order(2)
    @EnabledIfEnvironmentVariable(named="IAM_TENANT_LIVE_PERFORMANCE_TEST",matches="true")
    void onlineEvaluationAt100TenantsAnd1000Members() throws Exception {
        var accounts=new ArrayList<String>();for(int member=1;member<=1000;member++)accounts.add("("+(10000+member)+",'perf-"+member+"','not-a-credential')");
        jdbc.execute("INSERT INTO iam_account(id,username,password_hash) VALUES"+String.join(",",accounts));
        jdbc.execute("INSERT INTO iam_application(id,code,domain,name) VALUES(900000,'perf-tenant','TENANT','性能测试')");
        for(int resource=0;resource<10;resource++) {
            jdbc.execute("INSERT INTO iam_resource(id,application_id,code,name,scope_capabilities,field_capabilities) VALUES("+(910000+resource)+",900000,'r"+resource+"','资源"+resource+"','[\"ALL\"]','[]')");
            for(int action=0;action<10;action++)jdbc.execute("INSERT INTO iam_action(id,application_id,resource_id,code,name) VALUES("+(920000+resource*10+action)+",900000,"+(910000+resource)+",'perf-tenant:r"+resource+":op"+action+"','操作"+action+"')");
        }
        for(int role=0;role<4;role++) {
            int id=930000+role;
            jdbc.execute("INSERT INTO iam_role_definition(id,domain,kind,code,name) VALUES("+id+",'TENANT','SHARED','perf-"+role+"','性能角色"+role+"')");
            jdbc.execute("INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides) VALUES("+id+","+id+",'SHARED',1,'{}')");
            jdbc.execute("UPDATE iam_role_definition SET current_revision_id="+id+" WHERE id="+id);
            jdbc.execute("INSERT INTO iam_role_grant(revision_id,action_id,scopes) SELECT "+id+",id,'[{\"kind\":\"ALL\"}]' FROM iam_action WHERE application_id=900000");
        }
        for(int index=0;index<100;index++) {
            int tenant=10000+index,base=1000000+index*1000,group=940000+index;
            jdbc.execute("INSERT INTO iam_tenant(id,name) VALUES("+tenant+",'性能组织"+index+"')");
            var members=new ArrayList<String>();for(int member=1;member<=1000;member++)members.add("("+(base+member)+","+tenant+","+(10000+member)+",'成员"+member+"')");
            jdbc.execute("INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name) VALUES"+String.join(",",members));
            jdbc.execute("UPDATE iam_tenant SET owner_member_id="+(base+1)+" WHERE id="+tenant);
            jdbc.execute("INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,source) VALUES("+(950000+index)+","+tenant+",900000,TRUE,'INITIALIZATION')");
            jdbc.execute("INSERT INTO iam_app_audience(tenant_id,application_id,audience_kind) VALUES("+tenant+",900000,'ALL')");
            jdbc.execute("INSERT INTO iam_tenant_group(id,tenant_id,name) VALUES("+group+","+tenant+",'性能组')");
            jdbc.execute("INSERT INTO iam_tenant_group_member(tenant_id,group_id,member_id) VALUES("+tenant+","+group+","+(base+2)+")");
            for(int role=0;role<4;role++)jdbc.execute("INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,tenant_group_id,role_id,role_kind,role_tenant_key,scope_bindings,valid_from,source) VALUES("+(960000+index*4+role)+",'TENANT',"+tenant+",'"+(role<2?"MEMBER":"GROUP")+"',"+(role<2?Integer.toString(base+2):"NULL")+","+(role<2?"NULL":Integer.toString(group))+","+(930000+role)+",'SHARED',0,'{}','2000-01-01','MANUAL')");
            ACTORS.put("perf-"+index,new AuthorizationContext(AuthorizationDomain.TENANT,Integer.toString(tenant),"10002",Integer.toString(base+2)));
        }
        for(int warm=0;warm<100;warm++)assertEquals(100,codes(first,"perf-"+warm).size());
        long fourRoleSelects=selectsForCapabilities("perf-0");
        jdbc.update("UPDATE iam_role_assignment SET status='REVOKED' WHERE tenant_id=10000 AND role_id<>930000");
        long oneRoleSelects;
        try {oneRoleSelects=selectsForCapabilities("perf-0");}
        finally {jdbc.update("UPDATE iam_role_assignment SET status='ACTIVE' WHERE tenant_id=10000");}
        assertTrue(fourRoleSelects<=oneRoleSelects+1,"Role loading introduced per-role SELECTs");
        var sqlReads=Map.of("oneRoleSelects",oneRoleSelects,"fourRoleSelects",fourRoleSelects);
        var reports=new ArrayList<Map<String,Object>>();
        for(int concurrency:new int[]{20,100}) {
            var durations=new java.util.concurrent.ConcurrentLinkedQueue<Double>();var errors=new java.util.concurrent.atomic.AtomicInteger();
            long started=System.nanoTime(),end=started+Duration.ofSeconds(60).toNanos();
            try(var workers=java.util.concurrent.Executors.newFixedThreadPool(concurrency)) {
                var futures=new ArrayList<java.util.concurrent.Future<?>>();
                for(int worker=0;worker<concurrency;worker++) {final int identity=worker;
                    futures.add(workers.submit(()->{while(System.nanoTime()<end){long before=System.nanoTime();try{if(codes(first,"perf-"+identity).size()!=100)errors.incrementAndGet();}catch(Exception failure){errors.incrementAndGet();}durations.add((System.nanoTime()-before)/1_000_000.0);}}));}
                for(var future:futures)future.get();
            }
            var sorted=durations.stream().sorted().toList();double seconds=(System.nanoTime()-started)/1_000_000_000.0;
            var result=Map.<String,Object>of("concurrency",concurrency,"durationSeconds",seconds,"requests",sorted.size(),"errors",errors.get(),"throughput",sorted.size()/seconds,"p50Ms",sorted.get(sorted.size()/2),"p95Ms",sorted.get((int)(sorted.size()*0.95)));
            reports.add(result);System.out.println("TENANT_LIVE_PERFORMANCE "+json.writeValueAsString(result));
        }
        Files.writeString(Path.of("build/tenant-live-performance.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("tenants",100,"membersPerTenant",1000,"rolesPerIdentity",4,"actionsPerRole",100,"resources",10,"databasePool",64,"sqlReads",sqlReads,"reports",reports)));
        reports.forEach(result->{assertEquals(0,result.get("errors"));if(result.get("concurrency").equals(100))assertTrue((double)result.get("p95Ms")<=200,result.toString());});
    }

    @Test
    @Order(3)
    @EnabledIfEnvironmentVariable(named="IAM_TENANT_LIVE_BROWSER_TEST",matches="true")
    void actualBrowserComponentsAgainstLiveControllers() throws Exception {
        jdbc.update("UPDATE iam_app_audience SET audience_kind='ALL'");
        Path frontend=Path.of("").toAbsolutePath();while(!Files.exists(frontend.resolve("databases/iam/manifest.json")))frontend=frontend.getParent();frontend=frontend.resolveSibling("ingot-admin");
        var process=new ProcessBuilder("/Users/jy/.nvm/versions/node/v22.17.0/bin/node","scripts/verify-tenant-live-browser.mjs").directory(frontend.toFile()).redirectErrorStream(true);
        process.environment().put("IAM_FIXTURE_URL","http://127.0.0.1:"+first.getWebServer().getPort());
        process.environment().put("PLAYWRIGHT_CHROMIUM_EXECUTABLE","/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        var running=process.start();var output=new String(running.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);System.out.println(output);assertEquals(0,running.waitFor(),output);
    }
    /** 浏览器夹具目录适配器；角色保存、分配、会话仍调用生产控制器。 */
    @org.springframework.web.bind.annotation.RestController
    static class FixtureCatalog {
        private final IamAccess access;
        private final TenantAdministrationMapper live;
        FixtureCatalog(IamAccess access,TenantAdministrationMapper live){this.access=access;this.live=live;}
        @org.springframework.web.bind.annotation.GetMapping("/fixture/applications")
        com.ingot.framework.commons.model.support.R<AuthorizationCandidatePage> applications() {
            access.requireGoverned(AuthorizationDomain.PLATFORM,IamAction.PLATFORM_SHARED_ROLE_CREATE);
            var rows=live.applications(BigInteger.TEN);
            return com.ingot.framework.commons.model.support.R.ok(new AuthorizationCandidatePage(rows.stream().map(row->new AuthorizationOption(row.id().toString(),row.name(),null,null,null,null,null,null,null,false,null)).toList(),rows.size(),1,20,true,null));
        }
        @org.springframework.web.bind.annotation.GetMapping("/fixture/catalog")
        com.ingot.framework.commons.model.support.R<PageResponse<CurrentRoleCatalogAction>> catalog(@org.springframework.web.bind.annotation.RequestParam String applicationId,@org.springframework.web.bind.annotation.RequestParam(defaultValue="1") int page,@org.springframework.web.bind.annotation.RequestParam(defaultValue="20") int pageSize,@org.springframework.web.bind.annotation.RequestParam(defaultValue="") String keyword) {
            access.requireGoverned(AuthorizationDomain.PLATFORM,IamAction.PLATFORM_SHARED_ROLE_CREATE);
            var rows=live.ownerActions(BigInteger.TEN).stream().filter(row->row.applicationId().toString().equals(applicationId)&&row.actionName().contains(keyword)).toList();
            return com.ingot.framework.commons.model.support.R.ok(new PageResponse<>(rows.stream().skip((long)(page-1)*pageSize).limit(pageSize).map(row->new CurrentRoleCatalogAction(row.actionId().toString(),row.actionCode(),row.actionName(),row.applicationId().toString(),row.applicationName(),row.resourceId().toString(),row.resourceName(),true,com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec.read(row.scopeCapabilities(),new com.fasterxml.jackson.core.type.TypeReference<List<ScopeKind>>() { }),List.of())).toList(),rows.size(),page,pageSize));
        }
    }

    private LocalResourceAuthorizationClient registerResourceClients(AnnotationConfigServletWebServerApplicationContext web,
            DataSource source, CurrentIdentityService identities, AuthorizationEvaluator evaluator) {
        var candidates=IamMybatisTestAccess.mapper(source,AuthorizationCandidateMapper.class);
        var registry=new com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders(candidates).registry(List.of());
        var fields=new com.ingot.framework.authorization.field.FieldBindingRegistry(json);
        fields.register(MemberRecord.class,MemberResources.TENANT_MEMBER,IamAction.VALUE_TENANT_MEMBER_READ,com.ingot.framework.commons.annotation.field.FieldUse.READ);
        fields.register(MemberProfileInput.class,MemberResources.TENANT_MEMBER,IamAction.VALUE_TENANT_MEMBER_UPDATE,com.ingot.framework.commons.annotation.field.FieldUse.WRITE);
        var manifests=new com.ingot.cloud.iam.application.authorization.extension.FieldManifestService(fields,new com.ingot.framework.authorization.ResourceRpcProperties(),web,json,absent());
        var metadata=new com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata(IamMybatisTestAccess.mapper(source,IamApplicationMapper.class),IamMybatisTestAccess.mapper(source,IamResourceMapper.class),IamMybatisTestAccess.mapper(source,IamActionMapper.class),registry,manifests,absent());
        var compiler=new com.ingot.cloud.iam.infrastructure.persistence.AuthorizationScopeTransportCompiler(new DepartmentClosure(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class)),IamMybatisTestAccess.mapper(source,IamMemberDepartmentMapper.class));
        var tenantFields=com.ingot.cloud.iam.fixture.FieldAccessEvaluatorFixture.create(IamMybatisTestAccess.policies(source),IamMybatisTestAccess.mapper(source,IamMemberDepartmentMapper.class),new DepartmentClosure(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class)),absent(),new com.ingot.framework.authorization.field.FieldProjectionEngine(json,fields,new com.ingot.framework.authorization.field.DefaultMaskStrategy()),metadata,absent());
        var roleFields=new com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService(metadata,IamMybatisTestAccess.roles(source),compiler,evaluator,manifests,absent());
        var local=new LocalResourceAuthorizationClient(identities,evaluator,registry,compiler,roleFields,candidates,tenantFields,metadata,IamMybatisTestAccess.objectScopes(source));
        web.registerBean(com.ingot.cloud.iam.interfaces.inner.InnerResourceAuthorizationAPI.class,()->new com.ingot.cloud.iam.interfaces.inner.InnerResourceAuthorizationAPI(local));
        var remote=new com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService() {
            public com.ingot.framework.commons.model.support.R<com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO> snapshot(com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotRequest request){throw new UnsupportedOperationException();}
            public com.ingot.framework.commons.model.support.R<com.ingot.framework.commons.model.iam.extension.AuthorizationDecision> evaluate(com.ingot.framework.commons.model.iam.extension.AuthorizationRequest input){return call(input,"evaluate");}
            public com.ingot.framework.commons.model.support.R<com.ingot.framework.commons.model.iam.extension.AuthorizationDecision> preview(com.ingot.framework.commons.model.iam.extension.AuthorizationRequest input){return call(input,"preview");}
            private com.ingot.framework.commons.model.support.R<com.ingot.framework.commons.model.iam.extension.AuthorizationDecision> call(com.ingot.framework.commons.model.iam.extension.AuthorizationRequest input,String purpose) {
                try {
                    var actor=com.ingot.framework.authorization.RemoteAuthorizationClient.current();
                    var fixture=ACTORS.entrySet().stream().filter(entry->entry.getValue().equals(actor)).map(Map.Entry::getKey).findFirst().orElseThrow();
                    internalRpcCalls.incrementAndGet();
                    var response=request(first,fixture,"POST","/inner/authorization/v2/"+purpose,input);
                    return json.readValue(response.body(),new com.fasterxml.jackson.core.type.TypeReference<>() { });
                } catch(Exception failure){throw new com.ingot.framework.authorization.SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);}
            }
        };
        var cache=new com.ingot.framework.cache.spi.LayeredCache<String,com.ingot.framework.commons.model.iam.extension.AuthorizationDecision>() {
            public com.ingot.framework.commons.model.iam.extension.AuthorizationDecision get(String key){throw new AssertionError("Tenant consumed old allow cache");}
            public void evict(String key){throw new AssertionError("Tenant consumed old allow cache");}
            public void evictAll() { }
            public String name(){return "fixture-old-allow";}
        };
        var sdk=new com.ingot.framework.authorization.RemoteAuthorizationClient(remote,registry,cache,json);
        web.registerBean(com.ingot.framework.authorization.SdkAuthorizationErrorHandler.class,com.ingot.framework.authorization.SdkAuthorizationErrorHandler::new);
        web.registerBean(FixtureConsumer.class,()->new FixtureConsumer(sdk));
        return local;
    }
    @SuppressWarnings("unchecked")
    private static <T> org.springframework.beans.factory.ObjectProvider<T> absent() {return new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider((Class<T>)Object.class);}
    /** 独立消费节点只通过生产SDK取得决策，不读取IAM数据库或权限缓存。 */
    @org.springframework.web.bind.annotation.RestController
    static class FixtureConsumer {
        private final com.ingot.framework.authorization.AuthorizationClient sdk;
        FixtureConsumer(com.ingot.framework.authorization.AuthorizationClient sdk){this.sdk=sdk;}
        @org.springframework.web.bind.annotation.GetMapping("/fixture/consumer")
        com.ingot.framework.commons.model.support.R<com.ingot.framework.commons.model.iam.extension.AuthorizationDecision> get(@org.springframework.web.bind.annotation.RequestParam(defaultValue=IamAction.VALUE_TENANT_MEMBER_READ) String actionCode) {
            return com.ingot.framework.commons.model.support.R.ok(new com.ingot.framework.authorization.AuthorizationAccess(sdk).require(MemberResources.TENANT_MEMBER,actionCode));
        }
    }
    @Test
    @Order(4)
    void consumerSdkUsesRealInternalHttpAndOwnerPreservesCatalogFieldCeilings() throws Exception {
        jdbc.update("UPDATE iam_app_audience SET audience_kind='ALL'");
        String read=action(IamAction.VALUE_TENANT_MEMBER_READ);
        var created=request(first,"platform","POST","/v1/platform/shared-roles",Map.of("code","sdk-fixture","name","SDK角色","definition",definition(read)));
        assertEquals(200,created.statusCode(),created.body());String role=data(created).path("id").asText();assign("owner-a","102",role,null);
        long rpcBefore=internalRpcCalls.get();
        var allowed=request(second,"member-a","GET","/fixture/consumer",null);assertEquals(200,allowed.statusCode(),allowed.body());
        long rpcCount=internalRpcCalls.get()-rpcBefore;assertEquals(1,rpcCount);
        Files.writeString(Path.of("build/tenant-live-http-metrics.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("sdkInternalRpcCallsPerResourceRequest",rpcCount)));
        var owner=request(second,"owner-a","GET","/fixture/consumer?actionCode="+IamAction.VALUE_TENANT_MEMBER_UPDATE,null);assertEquals(200,owner.statusCode(),owner.body());
        assertEquals("MASKED",data(allowed).path("actions").path(IamAction.VALUE_TENANT_MEMBER_READ).path("fields").path("defaults").path("phone").path("visibility").asText());
        assertEquals("FULL",data(owner).path("actions").path(IamAction.VALUE_TENANT_MEMBER_UPDATE).path("fields").path("defaults").path("phone").path("visibility").asText());
        var resourceId=jdbc.queryForObject("SELECT r.id FROM iam_resource r JOIN iam_application a ON a.id=r.application_id WHERE a.code='iam-tenant' AND r.code='member'",Long.class);
        var latest=jdbc.queryForObject("SELECT id FROM iam_default_policy_revision WHERE kind='FIELD' AND resource_id=? ORDER BY revision DESC LIMIT 1",Long.class,resourceId);
        String policy=jdbc.queryForObject("SELECT definition FROM iam_default_policy_revision WHERE id=?",String.class,latest);
        var forced=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(policy);
        forced.putObject("ceiling").put("phone","MASKED");forced.putObject("operationCeiling").putObject("phone").put("editable",false).put("filterable",false);
        jdbc.update("INSERT INTO iam_default_policy_revision(id,kind,resource_id,revision,definition) VALUES(990001,'FIELD',?,2,?)",resourceId,json.writeValueAsString(forced));
        var limited=request(second,"owner-a","GET","/fixture/consumer?actionCode="+IamAction.VALUE_TENANT_MEMBER_UPDATE,null);assertEquals(200,limited.statusCode(),limited.body());
        var phone=data(limited).path("actions").path(IamAction.VALUE_TENANT_MEMBER_UPDATE).path("fields").path("defaults").path("phone");assertEquals("MASKED",phone.path("visibility").asText());assertFalse(phone.path("editable").asBoolean());
        jdbc.update("DELETE FROM iam_default_policy_revision WHERE id=990001");
        String lock=data(request(first,"platform","GET","/v1/platform/shared-roles/"+role,null)).path("version").asText();
        assertEquals(200,request(first,"platform","PUT","/v1/platform/shared-roles/"+role+"/grants",Map.of("expectedVersion",lock,"definition",definition())).statusCode());
        assertEquals(403,request(second,"member-a","GET","/fixture/consumer",null).statusCode());
        lock=data(request(first,"platform","GET","/v1/platform/shared-roles/"+role,null)).path("version").asText();request(first,"platform","PUT","/v1/platform/shared-roles/"+role+"/grants",Map.of("expectedVersion",lock,"definition",definition(read)));
        assertEquals(200,request(second,"member-a","GET","/fixture/consumer",null).statusCode());
        jdbc.execute("RENAME TABLE iam_role_definition TO iam_role_definition_outage");
        try {assertEquals(503,request(second,"member-a","GET","/fixture/consumer",null).statusCode());}
        finally {jdbc.execute("RENAME TABLE iam_role_definition_outage TO iam_role_definition");}
        assertEquals(200,request(second,"member-a","GET","/fixture/consumer",null).statusCode());
    }

    @Test
    @Order(5)
    void sharedSaveDmlDoesNotGrowWithAssignments() throws Exception {
        var created=request(first,"platform","POST","/v1/platform/shared-roles",Map.of("code","dml-fixture","name","写入预算角色","definition",definition(action(IamAction.VALUE_TENANT_MEMBER_READ))));
        assertEquals(200,created.statusCode(),created.body());String role=data(created).path("id").asText();
        for(int index=0;index<100;index++) {
            jdbc.update("INSERT INTO iam_account(id,username,password_hash) VALUES(?,?,'not-a-credential')",20000+index,"dml-"+index);
            jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name) VALUES(?,10,?,'预算成员')",700000+index,20000+index);
            jdbc.update("INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,role_id,role_kind,role_tenant_key,scope_bindings,valid_from,source) VALUES(?,'TENANT',10,'MEMBER',?,?,'SHARED',0,'{}','2000-01-01','MANUAL')",800000+index,700000+index,role);
        }
        long hundredAssignmentDml=dmlForSharedSave(role);
        jdbc.update("UPDATE iam_role_assignment SET status='REVOKED' WHERE role_id=? AND id<>800000",role);
        long oneAssignmentDml=dmlForSharedSave(role);
        assertTrue(oneAssignmentDml>0);assertEquals(oneAssignmentDml,hundredAssignmentDml);
        var metrics=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("build/tenant-live-http-metrics.json")));
        metrics.putObject("sharedSaveDml").put("oneActiveAssignment",oneAssignmentDml).put("hundredActiveAssignments",hundredAssignmentDml);
        Files.writeString(Path.of("build/tenant-live-http-metrics.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(metrics));
    }
    @Test
    @Order(6)
    void batchedAudienceAndEntitlementChangesApplyOnTheNextRequest() throws Exception {
        long application = 910001;
        long action = 910003;
        String actionCode = "audience-fixture:items:read";
        jdbc.update("INSERT INTO iam_application(id,code,domain,name) VALUES(?,'audience-fixture','TENANT','Audience')",
                application);
        jdbc.update("""
                INSERT INTO iam_resource(id,application_id,code,name,scope_capabilities,field_capabilities)
                VALUES(910002,?,'items','Items','["ALL"]','[]')
                """, application);
        jdbc.update("INSERT INTO iam_action(id,application_id,resource_id,code,name) VALUES(?,?,910002,?,'Read')",
                action, application, actionCode);
        jdbc.update("""
                INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,source)
                VALUES(910004,10,?,TRUE,'INITIALIZATION')
                """, application);
        jdbc.update("INSERT INTO iam_app_audience(tenant_id,application_id,audience_kind) VALUES(10,?,'SELECTED')",
                application);
        var created = request(first, "platform", "POST", "/v1/platform/shared-roles",
                Map.of("code", "audience-fixture", "name", "Audience", "definition", definition(Long.toString(action))));
        assertEquals(200, created.statusCode(), created.body());
        assign("owner-a", "104", data(created).path("id").asText(), null);
        assertAudience(application, actionCode, false);

        jdbc.update("INSERT INTO iam_audience_member(tenant_id,application_id,member_id) VALUES(10,?,104)", application);
        assertAudience(application, actionCode, true);
        jdbc.update("DELETE FROM iam_audience_member WHERE tenant_id=10 AND application_id=?", application);
        jdbc.update("""
                INSERT INTO iam_audience_department(tenant_id,application_id,department_id,include_descendants)
                VALUES(10,?,11,FALSE)
                """, application);
        assertAudience(application, actionCode, false);
        jdbc.update("UPDATE iam_audience_department SET include_descendants=TRUE WHERE tenant_id=10 AND application_id=?",
                application);
        assertAudience(application, actionCode, true);
        jdbc.update("DELETE FROM iam_audience_department WHERE tenant_id=10 AND application_id=?", application);

        jdbc.update("INSERT INTO iam_tenant_group(id,tenant_id,name) VALUES(910005,10,'Audience')");
        jdbc.update("INSERT INTO iam_audience_group(tenant_id,application_id,group_id) VALUES(10,?,910005)", application);
        jdbc.update("INSERT INTO iam_tenant_group_member(tenant_id,group_id,member_id) VALUES(10,910005,104)");
        assertAudience(application, actionCode, true);
        jdbc.update("DELETE FROM iam_tenant_group_member WHERE tenant_id=10 AND group_id=910005");
        jdbc.update("""
                INSERT INTO iam_tenant_group_department(tenant_id,group_id,department_id,include_descendants)
                VALUES(10,910005,11,TRUE)
                """);
        assertAudience(application, actionCode, true);

        var repository = IamMybatisTestAccess.entitlements(pool);
        BigInteger appId = BigInteger.valueOf(application);
        BigInteger tenant = BigInteger.TEN;
        BigInteger member = BigInteger.valueOf(104);
        var unlimited = repository.memberEntitlements(tenant, member, List.of(appId, appId));
        assertEquals(1, unlimited.size());
        assertNull(unlimited.get(appId).earliestExpiry());
        var otherTenant = repository.memberEntitlements(BigInteger.valueOf(20), BigInteger.valueOf(202),
                List.of(appId));
        assertTrue(otherTenant.isEmpty());
        assertTrue(repository.memberEntitlements(tenant, member, List.of()).isEmpty());

        jdbc.update("UPDATE iam_app_audience SET enabled=FALSE WHERE tenant_id=10 AND application_id=?", application);
        assertAudience(application, actionCode, false);
        jdbc.update("UPDATE iam_app_audience SET enabled=TRUE WHERE tenant_id=10 AND application_id=?", application);
        jdbc.update("""
                UPDATE iam_tenant_app_entitlement SET valid_from=UTC_TIMESTAMP(6)+INTERVAL 10 MINUTE
                WHERE tenant_id=10 AND application_id=?
                """, application);
        assertAudience(application, actionCode, false);
        var future = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.update("UPDATE iam_tenant_app_entitlement SET valid_from=UTC_TIMESTAMP(6),valid_until=? WHERE tenant_id=10 AND application_id=?",
                future, application);
        assertAudience(application, actionCode, true);
        assertEquals(future, repository.memberEntitlements(tenant, member, List.of(appId)).get(appId).earliestExpiry());
        jdbc.update("UPDATE iam_tenant_app_entitlement SET enabled=FALSE WHERE tenant_id=10 AND application_id=?", application);
        assertAudience(application, actionCode, false);
        jdbc.update("""
                UPDATE iam_tenant_app_entitlement SET enabled=TRUE,valid_from=NULL,valid_until=UTC_TIMESTAMP(6)
                WHERE tenant_id=10 AND application_id=?
                """, application);
        assertAudience(application, actionCode, false);
    }

    private void assertAudience(long application, String actionCode, boolean expected) throws Exception {
        assertEquals(expected, codes(second, "recipient-a").contains(actionCode));
        var response = request(second, "recipient-a", "GET", "/v1/me/bootstrap", null);
        assertEquals(200, response.statusCode(), response.body());
        var applications = data(response).path("applications");
        assertEquals(expected, applications.findValuesAsText("id").contains(Long.toString(application)));
    }

    @Test
    @Order(7)
    void catalogReadsOneSnapshotAndSeesCommittedWritesAcrossNodes() throws Exception {
        String platformApps = "/v1/platform/applications";
        var app = catalogResponse(first, "platform", "POST", platformApps + "/bundles", Map.of(
                "application", Map.of("code", "catalog-fixture", "domain", "TENANT", "name", "Catalog"),
                "resources", List.of(Map.of("tempId", "r1", "code", "items", "name", "Items",
                        "scopeCapabilities", List.of("ALL"), "fieldCapabilities", List.of(),
                        "actions", List.of(Map.of("tempId", "a1", "code", "read", "name", "Read")))),
                "menus", List.of()));
        String appId = app.path("id").asText();
        String appPath = platformApps + "/" + appId;
        String platformPath = appPath + "/action-catalog";
        String tenantPath = "/v1/tenant/applications/" + appId + "/action-catalog";
        jdbc.update("""
                INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,source)
                VALUES(910006,10,?,TRUE,'INITIALIZATION')
                """, appId);

        catalogResponse(first, "platform", "GET", platformPath, null);
        catalogResponse(first, "owner-a", "GET", tenantPath, null);
        long before = mysqlSelects();
        var platformView = catalogResponse(first, "platform", "GET", platformPath, null);
        assertEquals(7, mysqlSelects() - before);
        assertEquals(platformView, catalogResponse(second, "platform", "GET", platformPath, null));

        before = mysqlSelects();
        var tenantView = catalogResponse(first, "owner-a", "GET", tenantPath, null);
        assertEquals(9, mysqlSelects() - before);
        assertEquals(tenantView, catalogResponse(second, "owner-a", "GET", tenantPath, null));
        assertTrue(codes(second, "owner-a").contains("catalog-fixture:items:read"));
        assertNotEquals(200, request(second, "owner-b", "GET", tenantPath, null).statusCode());
        assertNotEquals(200, request(second, "member-a", "GET", tenantPath, null).statusCode());

        var initialResource = platformView.path("resources").get(0);
        String resourceId = initialResource.path("id").asText();
        String actionId = initialResource.path("actions").get(0).path("id").asText();
        catalogResponse(first, "platform", "PUT", appPath + "/actions/" + actionId,
                Map.of("expectedVersion", "0", "name", "Updated"));
        var updated = catalogResponse(second, "owner-a", "GET", tenantPath, null);
        assertTrue(updated.findValuesAsText("name").contains("Updated"));

        catalogResponse(first, "platform", "PATCH", appPath + "/actions/" + actionId,
                Map.of("expectedVersion", "1", "status", "DISABLED"));
        var tenantAfterDisable = catalogResponse(second, "owner-a", "GET", tenantPath, null);
        assertTrue(tenantAfterDisable.path("resources").isEmpty());
        assertFalse(codes(second, "owner-a").contains("catalog-fixture:items:read"));
        assertTrue(catalogResponse(second, "platform", "GET", platformPath, null)
                .findValuesAsText("status").contains("DISABLED"));

        catalogResponse(first, "platform", "DELETE", appPath + "/actions/" + actionId, null);
        assertTrue(catalogResponse(second, "platform", "GET", platformPath, null).findValues("status").isEmpty());
        catalogResponse(first, "platform", "PUT", appPath + "/resources/" + resourceId,
                Map.of("expectedVersion", "0", "name", "Updated resource", "scopeCapabilities", List.of("ALL"),
                        "fieldCapabilities", List.of()));
        assertTrue(catalogResponse(second, "platform", "GET", platformPath, null)
                .findValuesAsText("name").contains("Updated resource"));
        catalogResponse(first, "platform", "DELETE", appPath + "/resources/" + resourceId, null);
        assertTrue(catalogResponse(second, "platform", "GET", platformPath, null).path("resources").isEmpty());
        catalogResponse(first, "platform", "POST", appPath + "/resources",
                Map.of("code", "new", "name", "New", "scopeCapabilities", List.of("ALL"),
                        "fieldCapabilities", List.of()));
        String newResource = catalogResponse(second, "platform", "GET", platformPath, null)
                .path("resources").get(0).path("id").asText();
        catalogResponse(first, "platform", "POST", appPath + "/actions",
                Map.of("code", "read", "name", "New action", "resourceId", newResource));
        assertTrue(catalogResponse(second, "owner-a", "GET", tenantPath, null)
                .findValuesAsText("name").contains("New action"));

        jdbc.update("UPDATE iam_tenant_app_entitlement SET enabled=FALSE WHERE tenant_id=10 AND application_id=?", appId);
        assertNotEquals(200, request(second, "owner-a", "GET", tenantPath, null).statusCode());

        catalogResponse(first, "platform", "PUT", appPath, Map.of("expectedVersion", "0",
                "name", "New app name", "sortOrder", 0, "baseline", false));
        assertEquals("New app name", catalogResponse(second, "platform", "GET", platformPath, null)
                .path("applicationName").asText());
        catalogResponse(first, "platform", "PATCH", appPath,
                Map.of("expectedVersion", "1", "status", "DISABLED"));

        // 清除事务走生产用例，合成确认模拟 HTTP 解密后的输入。
        authenticateFixture(ACTORS.get("platform"));
        try {
            var purged = first.getBean(CatalogFacade.class).purgeApplication(appId, new ApplicationPurgeInput("2",
                    new SensitiveConfirmation(SensitiveConfirmationKind.LOGIN_PASSWORD, "synthetic")));
            assertEquals(appId, purged.id());
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertEquals(404, request(second, "platform", "GET", platformPath, null).statusCode());
    }

    @Test
    @Order(8)
    void bootstrapReadsMenuBindingsAndAdmissionFromOneLiveSnapshot() throws Exception {
        long app = 925001;
        long menu = 925005;
        jdbc.update("INSERT INTO iam_application(id,code,domain,name) VALUES(?,'menu-query-fixture','TENANT','Menus')", app);
        jdbc.update("""
                INSERT INTO iam_resource(id,application_id,code,name,scope_capabilities,field_capabilities)
                VALUES(925002,?,'items','Items','["ALL"]','[]')
                """, app);
        jdbc.update("""
                INSERT INTO iam_action(id,application_id,resource_id,code,name,enabled)
                VALUES(925003,?,925002,'menu-query-fixture:items:read','Read',TRUE),
                      (925004,?,925002,'menu-query-fixture:items:write','Write',FALSE)
                """, app, app);
        jdbc.update("""
                INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,source)
                VALUES(925006,10,?,TRUE,'INITIALIZATION')
                """, app);
        jdbc.update("""
                INSERT INTO iam_menu(id,application_id,name,path,kind,match_mode,access_mode,hidden)
                VALUES(?,?,'Bound','/bound','PAGE','ALL','ACTION',TRUE),
                      (925007,?,'Open','/open','PAGE','ANY','OPEN',FALSE)
                """, menu, app, app);
        assertMenu(second, menu, false);
        assertMenu(second, 925007, true);
        long before = mysqlSelects();
        assertMenu(second, menu, false);
        long withoutBindings = mysqlSelects() - before;

        jdbc.update("INSERT INTO iam_menu_action(application_id,menu_id,action_id) VALUES(?,?,925003),(?,?,925004)",
                app, menu, app, menu);
        before = mysqlSelects();
        assertMenu(second, menu, false);
        assertEquals(withoutBindings, mysqlSelects() - before);

        jdbc.update("UPDATE iam_menu SET match_mode='ANY' WHERE id=?", menu);
        assertMenu(second, menu, true);
        jdbc.update("DELETE FROM iam_menu_action WHERE menu_id=? AND action_id=925003", menu);
        assertMenu(second, menu, false);
        jdbc.update("UPDATE iam_menu SET access_mode='OPEN' WHERE id=?", menu);
        assertMenu(second, menu, true);
        jdbc.update("UPDATE iam_menu SET enabled=FALSE WHERE id=?", menu);
        assertMenu(second, menu, false);
        jdbc.update("UPDATE iam_tenant_app_entitlement SET enabled=FALSE WHERE application_id=?", app);
        assertMenu(second, 925007, false);
    }

    private void assertMenu(AnnotationConfigServletWebServerApplicationContext node, long menu, boolean expected) throws Exception {
        var bootstrap = catalogResponse(node, "owner-a", "GET", "/v1/me/bootstrap", null);
        assertEquals(expected, bootstrap.path("menus").findValuesAsText("id").contains(Long.toString(menu)));
    }

    private JsonNode catalogResponse(AnnotationConfigServletWebServerApplicationContext node,
            String actor, String method, String path, Object body) throws Exception {
        var response = request(node, actor, method, path, body);
        assertEquals(200, response.statusCode(), response.body());
        return data(response);
    }

    private long dmlForSharedSave(String role) throws Exception {
        var assignmentsBefore=jdbc.queryForList("SELECT * FROM iam_role_assignment WHERE role_id=? ORDER BY id",role);
        String lock=data(request(first,"platform","GET","/v1/platform/shared-roles/"+role,null)).path("version").asText();
        var definition=definition(action(IamAction.VALUE_TENANT_MEMBER_READ));long before=mysqlDml();
        var saved=request(first,"platform","PUT","/v1/platform/shared-roles/"+role+"/grants",Map.of("expectedVersion",lock,"definition",definition));
        long changed=mysqlDml()-before;assertEquals(200,saved.statusCode(),saved.body());
        assertEquals(assignmentsBefore,jdbc.queryForList("SELECT * FROM iam_role_assignment WHERE role_id=? ORDER BY id",role));
        return changed;
    }
    @Test
    @Order(50)
    void groupSummaryRetainsPageAndPermissionsWithoutLoadingBindings() throws Exception {
        jdbc.update("INSERT INTO iam_platform_group(id,name) VALUES (5101,'Group Platform'),(5102,'Group Platform 2')");
        jdbc.update("INSERT INTO iam_platform_group_member(group_id,member_id) VALUES (5101,1001)");
        jdbc.update("INSERT INTO iam_tenant_group(id,tenant_id,name) VALUES (5103,10,'Group Tenant'),(5104,10,'Group Tenant 2')");
        jdbc.update("INSERT INTO iam_tenant_group_member(tenant_id,group_id,member_id) VALUES (10,5103,104)");
        jdbc.update("INSERT INTO iam_tenant_group_department(tenant_id,group_id,department_id,include_descendants) VALUES (10,5103,11,TRUE)");
        for (String domain : List.of("platform", "tenant")) {
            String actor = domain.equals("platform") ? "platform" : "owner-a";
            String path = "/v1/" + domain + "/groups";
            var summaries = successfulGet(actor, path + "?name=Group&view=SUMMARY");
            var full = successfulGet(actor, path + "?name=Group");
            assertEquals(full.path("total"), summaries.path("total"));
            assertEquals(2, summaries.path("items").size());
            var summary = summaries.path("items").get(0);
            var detail = full.path("items").get(0);
            assertEquals(detail.path("record").path("id"), summary.path("record").path("id"));
            assertEquals(detail.path("version"), summary.path("version"));
            assertEquals(detail.path("capabilities"), summary.path("capabilities"));
            assertFalse(summary.path("record").has("selection"));
            assertEquals(1, detail.path("record").path("visibleMemberCount").asInt());
            assertEquals(1, detail.path("record").path("selection").path("members").size());
            long before = mysqlSelects();
            successfulGet(actor, path + "?name=Group&pageSize=1");
            long oneGroup = mysqlSelects() - before;
            before = mysqlSelects();
            successfulGet(actor, path + "?name=Group&pageSize=100");
            assertEquals(oneGroup, mysqlSelects() - before);
            assertTrue(successfulGet(actor, path + "?name=Absent&view=SUMMARY").path("items").isEmpty());
        }
        var otherTenant = successfulGet("owner-b", "/v1/tenant/groups?name=Group&view=SUMMARY");
        assertTrue(otherTenant.path("items").isEmpty());
    }

    @Test
    @Order(53)
    void representativeReadRoutesOnSeededDirectories() throws Exception {
        String platformApp = jdbc.queryForObject("SELECT id FROM iam_application WHERE code='iam-platform'", String.class);
        String tenantApp = jdbc.queryForObject("SELECT id FROM iam_application WHERE code='iam-tenant'", String.class);
        for (String path : List.of("/v1/me/bootstrap", "/v1/me/capabilities", "/v1/platform/shared-roles",
                "/v1/platform/members", "/v1/platform/members/context",
                "/v1/platform/applications?domain=PLATFORM",
                "/v1/platform/applications/" + platformApp,
                "/v1/platform/applications/" + platformApp + "/resources",
                "/v1/platform/applications/" + platformApp + "/menus",
                "/v1/platform/applications/" + platformApp + "/action-catalog")) {
            measureReadRoute("platform", path);
        }
        for (String path : List.of("/v1/me/bootstrap", "/v1/me/capabilities", "/v1/tenant/workbench/statistics",
                "/v1/tenant/roles", "/v1/tenant/assignments", "/v1/tenant/delegations",
                "/v1/tenant/members", "/v1/tenant/members/context", "/v1/tenant/members/picker",
                "/v1/tenant/applications", "/v1/tenant/applications/" + tenantApp + "/action-catalog",
                "/v1/tenant/roles/grant-catalog?applicationId=" + tenantApp)) {
            measureReadRoute("owner-a", path);
        }
    }

    @Test
    @Order(54)
    void catalogAndAssignmentWritesReportTheBusinessFailure() throws Exception {
        String apps = "/v1/platform/applications";
        var input = Map.of("code", "message-fixture", "domain", "PLATFORM", "name", "提示验收应用");
        String id = catalogResponse(first, "platform", "POST", apps, input).path("id").asText();
        assertFailure(request(first, "platform", "POST", apps, input),
                400, "InvalidArgument", "应用编码已存在");
        String app = apps + "/" + id;
        String menus = app + "/menus";
        var folder = Map.of("name", "验收目录", "kind", "DIRECTORY",
                "accessMode", "OPEN", "matchMode", "ANY", "actionIds", List.of());
        String parent = catalogResponse(first, "platform", "POST", menus, folder).path("id").asText();
        var invalidParent = new java.util.HashMap<String, Object>(folder);
        invalidParent.put("parentId", parent);
        assertFailure(request(first, "platform", "PUT", menus + "/" + parent,
                        Map.of("expectedVersion", "0", "menu", invalidParent)),
                400, "InvalidArgument", "菜单不能选择自身作为上级菜单");
        String child = catalogResponse(first, "platform", "POST", menus, invalidParent).path("id").asText();
        assertFailure(request(first, "platform", "DELETE", menus + "/" + parent, null),
                400, "ObjectInUse", "下仍有子菜单");
        assertFailure(request(first, "platform", "DELETE", app, null),
                400, "ObjectInUse", "下仍有菜单");
        catalogResponse(first, "platform", "DELETE", menus + "/" + child, null);
        catalogResponse(first, "platform", "DELETE", menus + "/" + parent, null);
        catalogResponse(first, "platform", "DELETE", app, null);
        assertFailure(request(second, "platform", "GET", app, null), 404, "ObjectNotFound", "应用不存在或不可访问");
        assertFailure(request(first, "owner-a", "DELETE", "/v1/tenant/assignments/invalid", null),
                400, "InvalidArgument", "无法撤销角色分配");

        String roles = "/v1/tenant/roles";
        var roleDraft = Map.of("code", "message-role", "name", "提示验收角色",
                "definition", definition(action(IamAction.VALUE_TENANT_MEMBER_READ)));
        String role = catalogResponse(first, "owner-a", "POST", roles, roleDraft).path("id").asText();
        assertFailure(request(first, "owner-a", "POST", roles, roleDraft),
                400, "InvalidArgument", "角色编码已存在");
        String assignments = "/v1/tenant/assignments";
        var missingRecipient = Map.of("items", List.of(Map.of("subject", Map.of("type", "MEMBER", "id", "99999"),
                "roleId", role, "scopeBindings", Map.of())));
        assertFailure(request(first, "owner-a", "POST", assignments, missingRecipient),
                404, "ObjectNotFound", "接收成员不存在或不可访问");
        var validAssignment = Map.of("items", List.of(Map.of("subject", Map.of("type", "MEMBER", "id", "102"),
                "roleId", role, "scopeBindings", Map.of())));
        String assignment = catalogResponse(first, "owner-a", "POST", assignments, validAssignment).path("id").asText();
        assertFailure(request(first, "owner-a", "DELETE", roles + "/" + role, null),
                400, "ObjectInUse", "该角色仍有分配记录");
        assertFailure(request(second, "owner-b", "GET", roles + "/" + role, null),
                404, "ObjectNotFound", "角色不存在或不可访问");
        catalogResponse(first, "owner-a", "DELETE", assignments + "/" + assignment, null);
    }

    private void assertFailure(HttpResponse<String> response, int status, String code, String message) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        var failure = json.readTree(response.body());
        assertEquals(code, failure.path("code").asText(), response.body());
        assertTrue(failure.path("message").asText().contains(message), response.body());
    }

    @Test
    @Order(55)
    @EnabledIfEnvironmentVariable(named = "IAM_TENANT_CONTEXT_BENCHMARK", matches = "true")
    void tenantContextAndAssignmentReadBenchmark() throws Exception {
        String permission = action(IamAction.VALUE_TENANT_MEMBER_READ);
        String roleId = catalogResponse(first, "owner-a", "POST", "/v1/tenant/roles",
                Map.of("code", "batch-probe", "name", "Batch probe", "definition", definition(permission))).path("id").asText();
        String resource = jdbc.queryForObject("SELECT resource_id FROM iam_action WHERE id=?", String.class, permission);
        String sourceId = catalogResponse(first, "owner-a", "POST", "/v1/tenant/delegations",
                Map.of("administratorMemberId", "103", "allowedRoleIds", List.of(roleId),
                        "recipientSelection", Map.of("members", List.of("102"), "departments", List.of()),
                        "resourceScopeCeilings", List.of(Map.of("resourceId", resource,
                                "scopes", List.of(Map.of("kind", "ALL")), "scopeBindings", Map.of())),
                        "maxAssignmentDuration", "P7D")).path("id").asText();
        jdbc.update("""
                INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,role_id,
                    role_kind,role_tenant_key,scope_bindings,valid_from,valid_until,delegation_grant_id,source,created_at)
                WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<120)
                SELECT 700000+n,'TENANT',10,'MEMBER',102,d.id,d.kind,10,'{}','2090-01-01','2090-01-02',
                    IF(MOD(n,2)=0,CAST(? AS UNSIGNED),NULL),'MANUAL','2020-01-01'
                FROM seq JOIN iam_role_definition d ON d.id=?
                """, sourceId, roleId);
        assertEquals(0, successfulGet("owner-b", "/v1/tenant/assignments").path("total").asInt());
        for (String path : List.of("/v1/tenant/members/context", "/v1/tenant/assignments?pageSize=1",
                "/v1/tenant/assignments?pageSize=20", "/v1/tenant/assignments?pageSize=100",
                "/v1/tenant/assignments?pageSize=100&page=2")) {
            var sample = successfulGet("owner-a", path);
            for (int i = 0; i < 6; i++) successfulGet("owner-a", path);
            var before = mysqlReadWorkload();
            var statements = statementCounts();
            List<Double> timings = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                long started = System.nanoTime();
                assertEquals(sample, successfulGet("owner-a", path));
                timings.add((System.nanoTime() - started) / 1_000_000.0);
            }
            timings.sort(Double::compareTo);
            var after = mysqlReadWorkload();
            System.out.println("IAM_CONTEXT_BENCH " + json.writeValueAsString(Map.of("path", path,
                    "p50Ms", percentile(timings, .50), "p95Ms", percentile(timings, .95),
                    "meanSelects", (after.selects() - before.selects()) / 15.0,
                    "meanSqlMs", (after.sqlMs() - before.sqlMs()) / 15.0,
                    "meanRows", (after.rows() - before.rows()) / 15.0, "sample", sample)));
            if (path.endsWith("/context")) {
                statementCounts().forEach((sql, count) -> {
                    long delta = count - statements.getOrDefault(sql, 0L);
                    if (delta > 0) System.out.println("IAM_CONTEXT_DIGEST " + delta + " " + sql);
                });
            }
        }
        if (Boolean.parseBoolean(System.getenv("IAM_ASSIGNMENT_VOLUME_BENCHMARK"))) {
            jdbc.update("""
                    INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,role_id,
                        role_kind,role_tenant_key,scope_bindings,valid_from,valid_until,delegation_grant_id,source,created_at)
                    WITH digits(n) AS (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
                        SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9),
                    seq(n) AS (SELECT a.n+10*b.n+100*c.n+1000*d.n+10000*e.n
                        FROM digits a CROSS JOIN digits b CROSS JOIN digits c CROSS JOIN digits d CROSS JOIN digits e)
                    SELECT 800000+n,'TENANT',10,'MEMBER',102,d.id,d.kind,10,'{}','2090-01-01','2090-01-02',
                        IF(MOD(n,2)=0,CAST(? AS UNSIGNED),NULL),'MANUAL','2020-01-01'
                    FROM seq JOIN iam_role_definition d ON d.id=? WHERE n<99880
                    """, sourceId, roleId);
            measureAssignmentVolume("first", "", null, 0, 100_000);
            measureAssignmentVolume("deep", "&page=4000", null, 79_980, 100_000);
            measureAssignmentVolume("name-hit", "&keyword=atch%20prob", "%atch prob%", 0, 100_000);
            measureAssignmentVolume("name-miss", "&keyword=missing-assignment", "%missing-assignment%", 0, 0);
        }
    }

    private void measureAssignmentVolume(String scenario, String query, String keyword, int offset, int total) throws Exception {
        var path = "/v1/tenant/assignments?pageSize=20" + query;
        var sample = successfulGet("owner-a", path);
        assertEquals(total, sample.path("total").asInt());
        assertEquals(Math.min(20, total), sample.path("items").size());
        if (total > 0) {
            long firstId = offset == 0 ? 700_001 : 800_000 + offset - 120;
            assertEquals(firstId, sample.path("items").get(0).path("record").path("id").asLong());
        }
        measureReadRoute("owner-a", path);
        var params = new HashMap<String, Object>(Map.of("tenantId", 10, "offset", offset, "limit", 20));
        params.put("keyword", keyword);
        explainAssignment(scenario, "count", com.ingot.cloud.iam.infrastructure.persistence.TenantAssignmentReadSql.COUNT, params);
        explainAssignment(scenario, "page", com.ingot.cloud.iam.infrastructure.persistence.TenantAssignmentReadSql.PAGE, params);
    }

    private void explainAssignment(String scenario, String statement, String script, Map<String, Object> params) throws Exception {
        var config = new org.apache.ibatis.session.Configuration();
        var sql = new org.apache.ibatis.scripting.xmltags.XMLLanguageDriver()
                .createSqlSource(config, script, Map.class).getBoundSql(params);
        var args = sql.getParameterMappings().stream().map(mapping -> params.get(mapping.getProperty())).toArray();
        System.out.println("IAM_ASSIGNMENT_EXPLAIN " + json.writeValueAsString(Map.of(
                "scenario", scenario, "statement", statement,
                "plan", jdbc.queryForObject("EXPLAIN ANALYZE " + sql.getSql(), String.class, args))));
    }

    @Test
    @Order(56)
    void batchAssignmentReadsReevaluateGroupDelegationAndPreserveTenantBoundaries() throws Exception {
        String rolePermission = action(IamAction.VALUE_TENANT_MEMBER_READ);
        String role = catalogResponse(first, "owner-a", "POST", "/v1/tenant/roles",
                Map.of("code", "batch-delegated-group", "name", "Batch delegated group",
                        "definition", definition(rolePermission))).path("id").asText();
        String resource = jdbc.queryForObject("SELECT resource_id FROM iam_action WHERE id=?", String.class, rolePermission);
        String delegation = catalogResponse(first, "owner-a", "POST", "/v1/tenant/delegations",
                Map.of("administratorMemberId", "103", "allowedRoleIds", List.of(role),
                        "recipientSelection", Map.of("members", List.of(), "departments",
                                List.of(Map.of("id", "11", "includeDescendants", true))),
                        "resourceScopeCeilings", List.of(Map.of("resourceId", resource,
                                "scopes", List.of(Map.of("kind", "ALL")), "scopeBindings", Map.of())),
                        "maxAssignmentDuration", "P7D")).path("id").asText();
        jdbc.update("INSERT INTO iam_tenant_group(id,tenant_id,name) VALUES(5110,10,'Batch inherited group')");
        jdbc.update("INSERT INTO iam_tenant_group_department(tenant_id,group_id,department_id,include_descendants) VALUES(10,5110,11,TRUE)");
        String path = "/v1/tenant/assignments";
        String assigned = catalogResponse(first, "delegate-a", "POST", path, Map.of("items", List.of(
                Map.of("subject", Map.of("type", "GROUP", "id", "5110"), "roleId", role, "scopeBindings", Map.of(),
                        "delegationGrantId", delegation, "validUntil", Instant.now().plusSeconds(3600).toString()))))
                .path("id").asText();
        assertEquals("ACTIVE", assignmentState("owner-a", path, assigned));
        assertEquals("ACTIVE", assignmentState("delegate-a", path, assigned));
        assertEquals("", assignmentState("owner-b", path, assigned));

        var groupSearch = path + "?keyword=Batch%20inherited&subjectType=";
        assertEquals(assigned, successfulGet("delegate-a", groupSearch + "GROUP")
                .path("items").get(0).path("record").path("id").asText());
        assertEquals(0, successfulGet("owner-a", groupSearch + "MEMBER").path("total").asInt());
        assertEquals(0, successfulGet("owner-b", groupSearch + "GROUP").path("total").asInt());
        jdbc.update("UPDATE iam_tenant_group SET name='Batch delegated group 100%_!' WHERE tenant_id=10 AND id=5110");
        assertEquals(1, successfulGet("owner-a", path + "?keyword=Batch%20delegated%20group").path("total").asInt());
        assertEquals(1, successfulGet("owner-a", path + "?keyword=100%25_%21").path("total").asInt());

        jdbc.update("INSERT INTO iam_tenant_group_member(tenant_id,group_id,member_id) VALUES(10,5110,102)");
        assertEquals("SOURCE_INVALID", assignmentState("owner-a", path, assigned));
        jdbc.update("DELETE FROM iam_tenant_group_member WHERE tenant_id=10 AND group_id=5110 AND member_id=102");
        assertEquals("ACTIVE", assignmentState("owner-a", path, assigned));
        catalogResponse(first, "owner-a", "DELETE", "/v1/tenant/delegations/" + delegation, null);
        assertEquals("SOURCE_INVALID", assignmentState("owner-a", path, assigned));
        jdbc.update("UPDATE iam_role_assignment SET valid_from='2020-01-01',valid_until='2020-01-02' WHERE tenant_id=10 AND id=?", assigned);
        assertEquals("EXPIRED", assignmentState("owner-a", path, assigned));
    }

    private String assignmentState(String actor, String path, String id) throws Exception {
        for (var item : successfulGet(actor, path + "?keyword=Batch%20delegated%20group").path("items")) {
            var record = item.path("record");
            if (id.equals(record.path("id").asText())) return record.path("effectiveStatus").asText();
        }
        return "";
    }

    private Map<String, Long> statementCounts() {
        var counts = new java.util.HashMap<String, Long>();
        jdbc.query("""
                SELECT DIGEST_TEXT, COUNT_STAR
                  FROM performance_schema.events_statements_summary_by_digest
                 WHERE SCHEMA_NAME='ingot_iam' AND (DIGEST_TEXT LIKE 'SELECT %' OR DIGEST_TEXT LIKE 'WITH %')
                   AND LOWER(DIGEST_TEXT) NOT LIKE '%events_statements_summary_by_digest%'
                """, (org.springframework.jdbc.core.RowCallbackHandler) row ->
                counts.put(row.getString(1), row.getLong(2)));
        return counts;
    }

    private void measureReadRoute(String actor, String path) throws Exception {
        long firstStarted = System.nanoTime();
        successfulGet(actor, path);
        double firstMs = (System.nanoTime() - firstStarted) / 1_000_000.0;
        var before = mysqlReadWorkload();
        List<Double> warm = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long start = System.nanoTime();
            successfulGet(actor, path);
            warm.add((System.nanoTime() - start) / 1_000_000.0);
        }
        warm.sort(Double::compare);
        var after = mysqlReadWorkload();
        System.out.println("IAM_READ_SMOKE " + json.writeValueAsString(Map.of(
                "actor", actor, "path", path, "firstMs", firstMs, "p95Ms", percentile(warm, .95),
                "p99Ms", percentile(warm, .99), "meanSelects", (after.selects() - before.selects()) / 20.0,
                "meanSqlMs", (after.sqlMs() - before.sqlMs()) / 20.0,
                "meanRows", (after.rows() - before.rows()) / 20.0)));
    }

    @Test
    @Order(52)
    @EnabledIfEnvironmentVariable(named="IAM_GROUP_BROWSER_SCRIPT", matches=".+")
    void groupWorkspaceBrowserAgainstMySql() throws Exception {
        var command = new ProcessBuilder(System.getenv().getOrDefault("IAM_GROUP_NODE", "node"),
                System.getenv("IAM_GROUP_BROWSER_SCRIPT")).redirectErrorStream(true);
        command.environment().put("IAM_FIXTURE_URL", "http://127.0.0.1:" + first.getWebServer().getPort());
        var process = command.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        System.out.println(output);
        assertEquals(0, process.waitFor(), output);
    }

    @Test
    @Order(51)
    @EnabledIfEnvironmentVariable(named="IAM_GROUP_READ_BENCHMARK",matches="true")
    void largeGroupReadBenchmark() throws Exception {
        var small = mysqlWorkload();
        successfulGet("owner-a", "/v1/me/capabilities");
        var smallAfter = mysqlWorkload();
        seedLargeUserAssociations();
        var large = mysqlWorkload();
        successfulGet("owner-a", "/v1/me/capabilities");
        var largeAfter = mysqlWorkload();
        System.out.println("IAM_GROUP_AUTH_SCALE " + json.writeValueAsString(Map.of(
                "smallSelects", smallAfter.selects() - small.selects(),
                "largeSelects", largeAfter.selects() - large.selects(),
                "smallRows", smallAfter.rows() - small.rows(),
                "largeRows", largeAfter.rows() - large.rows())));
        for (String domain : List.of("platform", "tenant")) {
            String actor = domain.equals("platform") ? "platform" : "owner-a";
            String groups = "/v1/" + domain + "/groups?name=Perf&pageSize=20&view=SUMMARY";
            String members = domain.equals("platform") ? "/v1/platform/groups/5200/members?pageSize=20"
                    : "/v1/tenant/groups/5200/effective-members?pageSize=20";
            for (int i = 0; i < 12; i++) successfulGet(actor, groups);
            long before = mysqlSelects();
            var page = successfulGet(actor, groups);
            long selects = mysqlSelects() - before;
            System.out.println("IAM_GROUP_PROBE " + json.writeValueAsString(Map.of(
                    "domain", domain, "selects", selects, "items", page.path("items").size(),
                    "responseBytes", json.writeValueAsBytes(page).length,
                    "view", page.path("items").get(0).path("record").has("selection") ? "full" : "summary")));
            successfulGet(actor, members);
            int selectedRound = Integer.parseInt(System.getenv().getOrDefault("IAM_GROUP_BENCHMARK_ROUND", "0"));
            for (int round = 1; round <= 3; round++) {
                if (selectedRound != 0 && selectedRound != round) continue;
                measureGroupReads(domain, actor, groups, null, 20, round);
                measureGroupReads(domain, actor, groups, members, 20, round);
            }
            if (selectedRound <= 1) measureGroupReads(domain, actor, groups, null, 100, 1);
        }
        for (String path : List.of("/v1/me/bootstrap", "/v1/me/capabilities", "/v1/tenant/workbench/statistics",
                "/v1/tenant/roles", "/v1/tenant/assignments", "/v1/tenant/delegations",
                "/v1/tenant/applications", "/v1/tenant/members", "/v1/tenant/members/context",
                "/v1/tenant/members/21000000/groups", "/v1/tenant/members/picker?pageSize=20",
                "/v1/tenant/groups?page=2&pageSize=100",
                "/v1/tenant/groups/5220/effective-members?page=400&pageSize=20")) {
            long before = mysqlSelects();
            long start = System.nanoTime();
            var response = request(first, "owner-a", "GET", path, null);
            System.out.println("IAM_GROUP_SCALE " + json.writeValueAsString(Map.of(
                    "path", path, "status", response.statusCode(), "selects", mysqlSelects() - before,
                    "ms", (System.nanoTime() - start) / 1_000_000.0)));
            assertEquals(200, response.statusCode(), response.body());
        }
        var largeGroup = successfulGet("owner-a", "/v1/tenant/groups?name=Large%2020&pageSize=1");
        var largeRecord = largeGroup.path("items").get(0).path("record");
        assertEquals(12250, largeRecord.path("selection").path("members").size());
        assertEquals(100000L, largeRecord.path("visibleMemberCount").asLong());
        if ("3".equals(System.getenv("IAM_GROUP_BENCHMARK_ROUND"))) {
            verifyRoleMemberPaginationAtScale();
        }
        for (String sql : List.of(
                "SELECT id FROM iam_tenant_group WHERE tenant_id=10 AND name LIKE '%Perf%' ORDER BY id LIMIT 20",
                "SELECT COUNT(*),SUM(version),MAX(id) FROM iam_tenant_member WHERE tenant_id=10",
                "SELECT id FROM iam_tenant_member WHERE tenant_id=10 AND status='ACTIVE' ORDER BY id LIMIT 20 OFFSET 80000",
                "SELECT member_id FROM iam_tenant_group_member WHERE tenant_id=10 AND group_id=5220 ORDER BY member_id LIMIT 20 OFFSET 8000")) {
            System.out.println("IAM_GROUP_EXPLAIN " + json.writeValueAsString(
                    Map.of("sql", sql, "plan", jdbc.queryForObject("EXPLAIN ANALYZE " + sql, String.class))));
        }
    }

    private void verifyRoleMemberPaginationAtScale() throws Exception {
        String permission = action(IamAction.VALUE_TENANT_MEMBER_READ);
        var created = request(first, "owner-a", "POST", "/v1/tenant/roles",
                Map.of("code", "scale-members", "name", "Scale members", "definition", definition(permission)));
        assertEquals(200, created.statusCode(), created.body());
        String roleId = data(created).path("id").asText();
        var grant = request(first, "owner-a", "POST", "/v1/tenant/assignments", Map.of("items", List.of(
                Map.of("subject", Map.of("type", "GROUP", "id", "5220"), "roleId", roleId, "scopeBindings", Map.of()))));
        assertEquals(200, grant.statusCode(), grant.body());
        String path = "/v1/tenant/roles/" + roleId + "/members";
        var before = mysqlWorkload();
        long start = System.nanoTime();
        var page = successfulGet("owner-a", path + "?page=4000&pageSize=20");
        var after = mysqlWorkload();
        assertEquals(20, page.path("items").size());
        assertEquals(100000L, page.path("total").asLong());
        System.out.println("IAM_GROUP_ROLE_MEMBERS " + json.writeValueAsString(Map.of(
                "total", page.path("total").asLong(), "selects", after.selects() - before.selects(),
                "rowsExamined", after.rows() - before.rows(), "ms", (System.nanoTime() - start) / 1_000_000.0)));
        assertEquals(404, request(second, "owner-b", "GET", path, null).statusCode());
    }

    private void seedLargeUserAssociations() {
        jdbc.execute("CREATE TABLE fixture_sequence (n INT NOT NULL PRIMARY KEY)");
        jdbc.execute("""
                INSERT INTO fixture_sequence (n)
                WITH digits AS (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
                  UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9)
                SELECT a.n+10*b.n+100*c.n+1000*d.n+10000*e.n
                  FROM digits a CROSS JOIN digits b CROSS JOIN digits c CROSS JOIN digits d CROSS JOIN digits e
                """);
        jdbc.execute("""
                INSERT INTO iam_account (id, username, password_hash)
                SELECT n+20000000, CONCAT('scale-fixture-',n), 'not-a-credential' FROM fixture_sequence
                """);
        jdbc.execute("""
                INSERT INTO iam_tenant_member (id, tenant_id, account_id, display_name)
                SELECT n+21000000,10,n+20000000,CONCAT('Member ',n) FROM fixture_sequence
                """);
        jdbc.execute("""
                INSERT INTO iam_platform_member (id, account_id, display_name)
                SELECT n+22000000,n+20000000,CONCAT('Platform member ',n) FROM fixture_sequence WHERE n < 10000
                """);
        jdbc.execute("INSERT INTO iam_tenant_group (id,tenant_id,name) SELECT 5200+n,10,CONCAT(IF(n < 20,'Perf ','Large '),n) FROM fixture_sequence WHERE n < 28");
        jdbc.execute("INSERT INTO iam_platform_group (id,name) SELECT 5200+n,CONCAT(IF(n < 20,'Perf ','Large '),n) FROM fixture_sequence WHERE n < 30");
        jdbc.execute("INSERT INTO iam_department (id,tenant_id,parent_id,name) SELECT 5300+n,10,IF(n=0,12,5299+n),CONCAT('Dept ',n) FROM fixture_sequence WHERE n < 9 ORDER BY n");
        jdbc.execute("""
                INSERT INTO iam_member_department (tenant_id,member_id,department_id,is_primary)
                SELECT 10,21000000+member.n,5300+dept.n,dept.n=0
                  FROM fixture_sequence member JOIN fixture_sequence dept ON dept.n < 9
                """);
        jdbc.execute("""
                INSERT INTO iam_tenant_group_member (tenant_id,group_id,member_id)
                SELECT 10,IF(n < 2000, 5200+(n DIV 100), 5220+((n-2000) DIV 12250)),21000000+n
                  FROM fixture_sequence
                """);
        jdbc.execute("""
                INSERT INTO iam_platform_group_member (group_id,member_id)
                SELECT 5200+grp.n,22000000+member.n
                  FROM fixture_sequence member
                  JOIN fixture_sequence grp ON grp.n < 30
                 WHERE (grp.n < 20 AND member.n < 100)
                    OR (grp.n >= 20 AND member.n < 9800)
                """);
        jdbc.execute("INSERT INTO iam_tenant_group_department (tenant_id,group_id,department_id,include_descendants) VALUES (10,5220,5307,TRUE)");
        jdbc.execute("DROP TABLE fixture_sequence");
        assertEquals(100000L, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_member WHERE id >= 21000000", Long.class));
        assertEquals(900000L, jdbc.queryForObject("SELECT COUNT(*) FROM iam_member_department WHERE member_id >= 21000000", Long.class));
        assertEquals(100000L, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_group_member WHERE member_id >= 21000000", Long.class));
        assertEquals(100000L, jdbc.queryForObject("SELECT COUNT(*) FROM iam_platform_group_member WHERE member_id >= 22000000", Long.class));
    }

    private com.fasterxml.jackson.databind.JsonNode successfulGet(String actor, String path) throws Exception {
        var response = request(first, actor, "GET", path, null);
        assertEquals(200, response.statusCode(), () -> path + " " + response.body());
        return data(response);
    }

    private void measureGroupReads(String domain, String actor, String groups, String members, int qps, int round)
            throws Exception {
        int durationSeconds = 4;
        long spacing = 1_000_000_000L / qps;
        long deadline = System.nanoTime() + spacing;
        long firstSend = deadline;
        var before = mysqlWorkload();
        int waiting = 0;
        List<Future<ReadTiming>> requests = new ArrayList<>();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < qps * durationSeconds; i++) {
                long planned = deadline;
                while (planned > System.nanoTime()) LockSupport.parkNanos(planned - System.nanoTime());
                waiting = Math.max(waiting, pool.getHikariPoolMXBean().getThreadsAwaitingConnection());
                requests.add(workers.submit(() -> {
                    long sent = System.nanoTime();
                    var response = request(first, actor, "GET", groups, null);
                    int bytes = response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    if (response.statusCode() != 200) return new ReadTiming(sent, 0, bytes, false);
                    if (members != null) {
                        response = request(first, actor, "GET", members, null);
                        bytes += response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    }
                    return new ReadTiming(sent, (System.nanoTime() - planned) / 1_000_000.0,
                            bytes, response.statusCode() == 200);
                }));
                deadline += spacing;
            }
            var samples = new ArrayList<ReadTiming>();
            for (var result : requests) samples.add(result.get());
            var durations = samples.stream().filter(ReadTiming::ok).map(ReadTiming::ms).sorted().toList();
            long errors = samples.size() - durations.size();
            var report = new java.util.LinkedHashMap<String, Object>();
            report.put("domain", domain);
            report.put("case", members == null ? "groups" : "groups-and-first-members");
            report.put("round", round);
            report.put("qps", qps);
            report.put("actualQps", samples.size() * 1e9 / Math.max(spacing * samples.size(),
                    samples.stream().mapToLong(ReadTiming::sent).max().orElseThrow() - firstSend + spacing));
            report.put("completedQps", samples.size() * 1e9 / (System.nanoTime() - firstSend + spacing));
            report.put("errors", errors);
            report.put("p50Ms", percentile(durations, .5));
            report.put("p95Ms", percentile(durations, .95));
            report.put("p99Ms", percentile(durations, .99));
            var after = mysqlWorkload();
            report.put("meanSelects", (after.selects() - before.selects()) / (double) samples.size());
            report.put("meanSelectMs", (after.sqlMs() - before.sqlMs()) / samples.size());
            report.put("meanRowsExamined", (after.rows() - before.rows()) / (double) samples.size());
            report.put("meanBytes", samples.stream().mapToInt(ReadTiming::bytes).average().orElseThrow());
            report.put("peakConnectionWaiters", waiting);
            System.out.println("IAM_GROUP_PERFORMANCE " + json.writeValueAsString(report));
            if (qps == 20) assertEquals(0L, errors, report.toString());
        }
    }

    private SqlWorkload mysqlReadWorkload() {
        return mysqlWorkload(true);
    }

    private SqlWorkload mysqlWorkload() {
        return mysqlWorkload(false);
    }

    private SqlWorkload mysqlWorkload(boolean includeCtes) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(COUNT_STAR),0) AS queries,
                       COALESCE(SUM(SUM_TIMER_WAIT),0)/1000000000 AS milliseconds,
                       COALESCE(SUM(SUM_ROWS_EXAMINED),0) AS inspected
                  FROM performance_schema.events_statements_summary_by_digest
                 WHERE SCHEMA_NAME='ingot_iam' AND (DIGEST_TEXT LIKE 'SELECT %' OR (? AND DIGEST_TEXT LIKE 'WITH %'))
                   AND LOWER(DIGEST_TEXT) NOT LIKE '%events_statements_summary_by_digest%'
                """, (row, index) -> new SqlWorkload(row.getLong("queries"),
                row.getDouble("milliseconds"), row.getLong("inspected")), includeCtes);
    }

    private record SqlWorkload(long selects, double sqlMs, long rows) { }

    private static double percentile(List<Double> values, double quantile) {
        return values.isEmpty() ? -1 : values.get((int) Math.ceil(values.size() * quantile) - 1);
    }

    private record ReadTiming(long sent, double ms, int bytes, boolean ok) { }

    private long mysqlDml() {
        return mysqlCounter("Com_insert")+mysqlCounter("Com_update")+mysqlCounter("Com_delete");
    }
    private long mysqlCounter(String status) {
        return jdbc.queryForObject("SHOW GLOBAL STATUS LIKE '"+status+"'",(row,index)->Long.parseLong(row.getString("Value")));
    }

    private long selectsForCapabilities(String actor) throws Exception {
        long before=mysqlSelects();assertEquals(100,codes(first,actor).size());return mysqlSelects()-before;
    }
    private long mysqlSelects() {
        return jdbc.queryForObject("SHOW GLOBAL STATUS LIKE 'Com_select'",(row,index)->Long.parseLong(row.getString("Value")));
    }
    @Test
    @Order(57)
    void tenantMemberEditHintsMatchTheScopedFieldWriteDecision() throws Exception {
        String actor = "field-editor";
        String target = "/v1/tenant/members/14811";
        String phone = "15500014811";
        String email = "field-editor@example.test";
        String read = action(IamAction.VALUE_TENANT_MEMBER_READ);
        String update = action(IamAction.VALUE_TENANT_MEMBER_UPDATE);
        String resource = jdbc.queryForObject("SELECT resource_id FROM iam_action WHERE id=?",
                BigInteger.class, new BigInteger(update)).toString();
        jdbc.update("INSERT INTO iam_account(id,username,password_hash) VALUES(14801,'field-editor','fixture')");
        jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name,phone,email) "
                + "VALUES(14811,10,14801,'字段验证',?,?)", phone, email);
        jdbc.update("INSERT INTO iam_member_department(tenant_id,member_id,department_id) VALUES(10,14811,12)");
        ACTORS.put(actor, new AuthorizationContext(AuthorizationDomain.TENANT, "10", "14801", "14811"));
        try {
            String readRole = catalogResponse(first, "owner-a", "POST", "/v1/tenant/roles",
                    Map.of("code", "field-reader", "name", "仅查看", "definition", definition(read))).path("id").asText();
            assign("owner-a", "14811", readRole, null);
            var readOnly = catalogResponse(second, actor, "GET", target, null);
            assertEquals(FieldVisibility.MASKED.name(), readOnly.at("/fieldAccess/phone/visibility").asText());
            assertFalse(readOnly.at("/fieldAccess/displayName/editable").booleanValue());

            var definition = Map.of("grants", List.of(Map.of("actionId", update,
                    "scopes", List.of(Map.of("kind", ScopeKind.MEMBER_DEPARTMENTS.getValue())))),
                    "parameterDefinitions", List.of());
            String editRole = catalogResponse(first, "owner-a", "POST", "/v1/tenant/roles",
                    Map.of("code", "field-editor", "name", "部门内编辑", "definition", definition)).path("id").asText();
            // 角色创建夹具没有注册字段资源，直接预置当前快照以检验真实授权求值。
            var snapshot = Map.of(resource, Map.of(
                    "visibility", Map.of("displayName", "FULL", "avatar", "FULL", "phone", "MASKED", "email", "MASKED"),
                    "operations", Map.of("displayName", new FieldOperations(true, false),
                            "avatar", new FieldOperations(true, false), "phone", FieldOperations.NONE,
                            "email", new FieldOperations(true, false))));
            assertEquals(1, jdbc.update("UPDATE iam_role_revision SET resource_field_permissions=? "
                    + "WHERE id=(SELECT current_revision_id FROM iam_role_definition WHERE id=?)",
                    json.writeValueAsString(snapshot), editRole));
            assign("owner-a", "14811", editRole, null);
            var before = catalogResponse(second, actor, "GET", target, null);
            String version = before.path("version").asText();
            assertEquals(FieldVisibility.MASKED.name(), before.at("/fieldAccess/phone/visibility").asText());
            assertFalse(before.at("/fieldAccess/phone/editable").booleanValue());
            assertTrue(before.at("/record/phone").asText().contains("*"));
            assertTrue(before.at("/fieldAccess/displayName/editable").booleanValue());
            assertEquals(FieldVisibility.MASKED.name(), before.at("/fieldAccess/email/visibility").asText());
            assertTrue(before.at("/fieldAccess/email/editable").booleanValue());
            assertTrue(before.at("/record/email").asText().contains("*"));
            var outside = catalogResponse(second, actor, "GET", "/v1/tenant/members/102", null);
            assertFalse(outside.at("/fieldAccess/displayName/editable").booleanValue());
            assertTrue(catalogResponse(second, "owner-a", "GET", target, null)
                    .at("/fieldAccess/phone/editable").booleanValue());

            var denied = request(first, actor, "PATCH", target,
                    Map.of("expectedVersion", version, "phone", "15500014812"));
            assertEquals(403, denied.statusCode(), denied.body());
            assertEquals(phone, jdbc.queryForObject("SELECT phone FROM iam_tenant_member WHERE id=14811", String.class));
            assertEquals(version, catalogResponse(first, actor, "GET", target, null).path("version").asText());

            expireMemberWritePreview.set(true);
            var expired = catalogResponse(first, actor, "GET", target, null);
            assertEquals(before.path("record"), expired.path("record"));
            expired.path("fieldAccess").forEach(field -> assertFalse(field.path("editable").booleanValue()));
            var saved = catalogResponse(first, actor, "PATCH", target,
                    Map.of("expectedVersion", version, "displayName", "已更新"));
            assertEquals("已更新", saved.at("/record/displayName").asText());
            assertNotEquals(version, saved.path("version").asText());
            assertEquals(before.path("fieldAccess"), saved.path("fieldAccess"));
            assertEquals(before.at("/record/phone"), saved.at("/record/phone"));
        }
        finally {
            expireMemberWritePreview.set(false);
            ACTORS.remove(actor);
        }
    }

    private String action(String code){return jdbc.queryForObject("SELECT id FROM iam_action WHERE code=?",BigInteger.class,code).toString();}
    private Map<String,Object> definition(String... actions){return Map.of("grants",Arrays.stream(actions).map(id->Map.of("actionId",id,"scopes",List.of(Map.of("kind","ALL")))).toList(),"parameterDefinitions",List.of());}
    private void assign(String actor,String member,String role,String source) throws Exception {
        Map<String,Object> item=new LinkedHashMap<>();item.put("subject",Map.of("type","MEMBER","id",member));item.put("roleId",role);item.put("scopeBindings",Map.of());if(source!=null){item.put("delegationGrantId",source);item.put("validUntil",Instant.now().plusSeconds(3600).toString());}
        var response=request(first,actor,"POST","/v1/tenant/assignments",Map.of("items",List.of(item)));assertEquals(200,response.statusCode(),response.body());
    }
    private List<String> codes(AnnotationConfigServletWebServerApplicationContext node,String actor) throws Exception {var response=request(node,actor,"GET","/v1/me/capabilities",null);assertEquals(200,response.statusCode(),response.body());List<String> codes=new ArrayList<>();data(response).path("actionCodes").forEach(v->codes.add(v.asText()));return codes;}
    private JsonNode data(HttpResponse<String> response) throws Exception{return json.readTree(response.body()).path("data");}
    private HttpResponse<String> request(AnnotationConfigServletWebServerApplicationContext node,String actor,String method,String path,Object body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+node.getWebServer().getPort()+path)).header("X-Fixture-Actor",actor).header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    private static String command(String... args) throws Exception {var process=new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.INHERIT).start();String text=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();if(process.waitFor()!=0)throw new IllegalStateException("隔离测试命令失败");return text;}
    @Configuration
    @EnableWebMvc
    static class Mvc implements WebMvcConfigurer {
        @Override public void configureMessageConverters(List<HttpMessageConverter<?>> converters){converters.add(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules().registerModule(new com.ingot.framework.commons.jackson.InApiTimeModule())));}
    }
}
