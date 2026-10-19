package com.ingot.cloud.iam.live;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.*;
import com.ingot.cloud.iam.assignment.TenantLiveAssignmentService;
import com.ingot.cloud.iam.authorization.read.IamReadRequestConfiguration;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.delegation.TenantLiveDelegationService;
import com.ingot.cloud.iam.evaluation.*;
import com.ingot.cloud.iam.identity.*;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.cloud.iam.role.*;
import com.ingot.cloud.iam.session.SessionService;
import com.ingot.cloud.iam.support.*;
import com.ingot.cloud.iam.web.IamErrorHandler;
import com.ingot.cloud.iam.web.v1.CurrentSessionAPI;
import com.ingot.cloud.iam.web.v1.tenant.*;
import com.ingot.cloud.iam.web.v1.platform.PlatformSharedRoleCommandAPI;
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
        var source=new DriverManagerDataSource("jdbc:mysql://"+address+"/fixture?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&useSSL=false","root","");
        jdbc=new JdbcTemplate(source);long deadline=System.nanoTime()+Duration.ofSeconds(60).toNanos();
        while(true){try{jdbc.execute("SELECT 1");break;}catch(Exception e){if(System.nanoTime()>deadline)throw e;Thread.sleep(500);}}
        pool=new com.zaxxer.hikari.HikariDataSource();pool.setJdbcUrl(source.getUrl());pool.setUsername("root");pool.setPassword("");pool.setMaximumPoolSize(64);pool.setMinimumIdle(8);pool.setConnectionTimeout(5000);jdbc=new JdbcTemplate(pool);
        Path root=Path.of("").toAbsolutePath();while(!Files.exists(root.resolve("databases/iam/manifest.json")))root=root.getParent();
        new ResourceDatabasePopulator(new FileSystemResource(root.resolve("databases/ingot_iam.sql"))).execute(pool);
        jdbc.update("INSERT INTO iam_account(id,username,password_hash) VALUES(1,'fixture-1','not-a-credential'),(2,'fixture-2','not-a-credential'),(3,'fixture-3','not-a-credential'),(4,'fixture-4','not-a-credential')");
        jdbc.update("INSERT INTO iam_platform_member(id,account_id,display_name) VALUES(1001,1,'平台')");
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,revision_id,revision_kind,scope_bindings,valid_from,source) SELECT 900,'PLATFORM','MEMBER',1001,r.id,'SYSTEM','{}',UTC_TIMESTAMP(6),'INITIALIZATION' FROM iam_role_revision r JOIN iam_role_definition d ON d.id=r.role_id WHERE d.domain='PLATFORM' AND d.kind='SYSTEM' ORDER BY r.revision DESC LIMIT 1");
        jdbc.update("INSERT INTO iam_tenant(id,name) VALUES(10,'组织A'),(20,'组织B')");
        jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name) VALUES(101,10,1,'拥有者A'),(102,10,2,'成员A'),(103,10,3,'管理员A'),(104,10,4,'接收人A'),(201,20,1,'拥有者B'),(202,20,2,'成员B')");
        jdbc.update("UPDATE iam_tenant SET owner_member_id=CASE id WHEN 10 THEN 101 ELSE 201 END");
        jdbc.update("INSERT INTO iam_department(id,tenant_id,parent_id,name) VALUES(11,10,NULL,'研发'),(12,10,11,'研发子部门'),(13,10,NULL,'营销')");
        jdbc.update("INSERT INTO iam_member_department(tenant_id,member_id,department_id) VALUES(10,104,12)");
        jdbc.update("INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,valid_from,source) SELECT 600+tenant.id,tenant.id,app.id,TRUE,'2000-01-01','INITIALIZATION' FROM iam_tenant tenant JOIN iam_application app ON app.code='iam-tenant'");
        jdbc.update("INSERT INTO iam_field_policy(tenant_id,default_revision_id) SELECT t.id,d.id FROM iam_tenant t JOIN iam_default_policy_revision d ON d.kind='FIELD' WHERE d.revision=(SELECT MAX(revision) FROM iam_default_policy_revision WHERE kind='FIELD')");
        jdbc.update("INSERT INTO iam_app_audience(tenant_id,application_id,audience_kind) SELECT tenant.id,app.id,'ALL' FROM iam_tenant tenant JOIN iam_application app ON app.code='iam-tenant'");
        first=node(pool);second=node(pool);
    }
    private AnnotationConfigServletWebServerApplicationContext node(DataSource source) {
        var web=new AnnotationConfigServletWebServerApplicationContext();
        var evaluator=IamMybatisTestAccess.evaluator(source);
        var identities=new CurrentIdentityService(new ActiveIdentityService(IamMybatisTestAccess.identity(source)));
        var changes=new AuthorizationChangeNotifier(event->{ });
        var access=new IamAccess(identities,evaluator,ids::incrementAndGet,new PlatformAdministratorGuard(IamMybatisTestAccess.assignments(source),changes));
        var live=IamMybatisTestAccess.mapper(source,TenantLiveMapper.class);
        var resources=IamMybatisTestAccess.resourceAccess(source);
        var transaction=new TenantLiveTransaction(new DataSourceTransactionManager(source));
        var roleService=new TenantCurrentRoleService(access,resources,live,IamMybatisTestAccess.currentRoles(source),new RoleGrantValidator(IamMybatisTestAccess.roles(source)),transaction,IamMybatisTestAccess.audits(source),changes);
        var assignmentService=new TenantLiveAssignmentService(access,live,IamMybatisTestAccess.currentRoles(source),roleService,resources,transaction,IamMybatisTestAccess.audits(source),changes,IamMybatisTestAccess.mapper(source,TenantScopeCandidateMapper.class),new DepartmentClosure(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class)));
        var delegationService=new TenantLiveDelegationService(access,live,IamMybatisTestAccess.currentRoles(source),assignmentService,resources,transaction,IamMybatisTestAccess.audits(source),changes);
        var sessionStore=new SessionRepository(IamMybatisTestAccess.mapper(source,IamPlatformMemberMapper.class),IamMybatisTestAccess.mapper(source,IamTenantMemberMapper.class),IamMybatisTestAccess.mapper(source,IamApplicationMapper.class),IamMybatisTestAccess.mapper(source,IamActionMapper.class),IamMybatisTestAccess.mapper(source,IamMenuMapper.class),IamMybatisTestAccess.mapper(source,IamMenuActionMapper.class),IamMybatisTestAccess.mapper(source,IamTenantAppEntitlementMapper.class));
        var sessions=new SessionService(access,evaluator,sessionStore,live);
        registerResourceClients(web,source,identities,evaluator);
        web.registerBean(ServletWebServerFactory.class,()->new TomcatServletWebServerFactory(0));
        web.registerBean(TenantRoleCommandAPI.class,()->new TenantRoleCommandAPI(roleService,identities));
        web.registerBean(PlatformSharedRoleCommandAPI.class,()->new PlatformSharedRoleCommandAPI(roleService,identities));
        web.registerBean(TenantAssignmentAPI.class,()->new TenantAssignmentAPI(assignmentService));
        web.registerBean(TenantDelegationAPI.class,()->new TenantDelegationAPI(delegationService));
        web.registerBean(FixtureCatalog.class,()->new FixtureCatalog(access,live));
        web.registerBean(CurrentSessionAPI.class,()->new CurrentSessionAPI(sessions,org.mockito.Mockito.mock(com.ingot.cloud.iam.account.CurrentAccountService.class)));
        web.registerBean(IamErrorHandler.class,IamErrorHandler::new);
        web.registerBean(com.ingot.cloud.iam.web.TenantCurrentContractErrorHandler.class,com.ingot.cloud.iam.web.TenantCurrentContractErrorHandler::new);
        web.registerBean(DispatcherServletRegistrationBean.class,()->new DispatcherServletRegistrationBean(new DispatcherServlet(web),"/"));
        web.registerBean(FilterRegistrationBean.class,()->new FilterRegistrationBean<>(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,jakarta.servlet.FilterChain chain) throws java.io.IOException,jakarta.servlet.ServletException {
                var actor=ACTORS.get(request.getHeader("X-Fixture-Actor"));
                if(actor==null){response.setStatus(401);return;}
                var user=InUser.stateless(Long.parseLong(actor.accountId()),actor.tenantId()==null?null:Long.parseLong(actor.tenantId()),"web","standard",UserTypeEnum.ADMIN.getValue(),"fixture",List.of(),List.of(),Map.of()).toBuilder().authorizationContext(actor).build();
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,List.of()));
                try{chain.doFilter(request,response);}finally{SecurityContextHolder.clearContext();}
            }}));
        web.register(Mvc.class,IamReadRequestConfiguration.class);web.refresh();return web;
    }
    @AfterAll
    void stop() throws Exception {if(first!=null)first.close();if(second!=null)second.close();if(pool!=null)pool.close();if(container!=null)command("docker","stop","--timeout","10",container);}

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
        private final TenantLiveMapper live;
        FixtureCatalog(IamAccess access,TenantLiveMapper live){this.access=access;this.live=live;}
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
            return com.ingot.framework.commons.model.support.R.ok(new PageResponse<>(rows.stream().skip((long)(page-1)*pageSize).limit(pageSize).map(row->new CurrentRoleCatalogAction(row.actionId().toString(),row.actionCode(),row.actionName(),row.applicationId().toString(),row.applicationName(),row.resourceId().toString(),row.resourceName(),true,com.ingot.cloud.iam.support.IamJson.read(row.scopeCapabilities(),new com.fasterxml.jackson.core.type.TypeReference<List<ScopeKind>>() { }),List.of())).toList(),rows.size(),page,pageSize));
        }
    }

    private void registerResourceClients(AnnotationConfigServletWebServerApplicationContext web,DataSource source,CurrentIdentityService identities,AuthorizationEvaluator evaluator) {
        var candidates=IamMybatisTestAccess.mapper(source,AuthorizationCandidateMapper.class);
        var registry=new com.ingot.cloud.iam.extension.BuiltinResourceProviders(candidates).registry(List.of());
        var fields=new com.ingot.framework.authorization.field.FieldBindingRegistry(json);
        fields.register(MemberRecord.class,MemberResources.TENANT_MEMBER,IamAction.VALUE_TENANT_MEMBER_READ,com.ingot.framework.commons.annotation.field.FieldUse.READ);
        fields.register(MemberProfileInput.class,MemberResources.TENANT_MEMBER,IamAction.VALUE_TENANT_MEMBER_UPDATE,com.ingot.framework.commons.annotation.field.FieldUse.WRITE);
        var manifests=new com.ingot.cloud.iam.extension.FieldManifestService(fields,new com.ingot.framework.authorization.ResourceRpcProperties(),web,json,absent());
        var metadata=new com.ingot.cloud.iam.extension.ResourceFieldMetadata(IamMybatisTestAccess.mapper(source,IamApplicationMapper.class),IamMybatisTestAccess.mapper(source,IamResourceMapper.class),IamMybatisTestAccess.mapper(source,IamActionMapper.class),registry,manifests,absent());
        var compiler=new com.ingot.cloud.iam.extension.ScopeTransportCompiler(new DepartmentClosure(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class)),IamMybatisTestAccess.mapper(source,IamMemberDepartmentMapper.class));
        var tenantFields=new com.ingot.cloud.iam.policy.FieldAccessEvaluator(IamMybatisTestAccess.policies(source),IamMybatisTestAccess.mapper(source,IamMemberDepartmentMapper.class),new DepartmentClosure(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class)),absent(),new com.ingot.framework.authorization.field.FieldProjectionEngine(json,fields,new com.ingot.framework.authorization.field.DefaultMaskStrategy()),metadata,absent());
        var roleFields=new com.ingot.cloud.iam.extension.RoleFieldPermissionService(metadata,IamMybatisTestAccess.roles(source),compiler,evaluator,manifests,absent());
        var local=new com.ingot.cloud.iam.extension.LocalResourceAuthorizationClient(identities,evaluator,registry,compiler,roleFields,candidates,tenantFields,IamMybatisTestAccess.objectScopes(source));
        web.registerBean(com.ingot.cloud.iam.web.inner.InnerResourceAuthorizationAPI.class,()->new com.ingot.cloud.iam.web.inner.InnerResourceAuthorizationAPI(local));
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
        var latest=jdbc.queryForObject("SELECT id FROM iam_default_policy_revision WHERE kind='FIELD' ORDER BY revision DESC LIMIT 1",Long.class);
        String policy=jdbc.queryForObject("SELECT definition FROM iam_default_policy_revision WHERE id=?",String.class,latest);
        var forced=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(policy);
        forced.putObject("ceiling").put("phone","MASKED");forced.putObject("operationCeiling").putObject("phone").put("editable",false).put("filterable",false);
        jdbc.update("INSERT INTO iam_default_policy_revision(id,kind,revision,definition) VALUES(990001,'FIELD',2,?)",json.writeValueAsString(forced));
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
    private long dmlForSharedSave(String role) throws Exception {
        var assignmentsBefore=jdbc.queryForList("SELECT * FROM iam_role_assignment WHERE role_id=? ORDER BY id",role);
        String lock=data(request(first,"platform","GET","/v1/platform/shared-roles/"+role,null)).path("version").asText();
        var definition=definition(action(IamAction.VALUE_TENANT_MEMBER_READ));long before=mysqlDml();
        var saved=request(first,"platform","PUT","/v1/platform/shared-roles/"+role+"/grants",Map.of("expectedVersion",lock,"definition",definition));
        long changed=mysqlDml()-before;assertEquals(200,saved.statusCode(),saved.body());
        assertEquals(assignmentsBefore,jdbc.queryForList("SELECT * FROM iam_role_assignment WHERE role_id=? ORDER BY id",role));
        return changed;
    }
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
