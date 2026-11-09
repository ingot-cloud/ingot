package com.ingot.cloud.iam.infrastructure.persistence;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.github.yulichang.injector.MPJSqlInjector;
import com.github.yulichang.interceptor.MPJInterceptor;
import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.framework.security.account.adapter.mapper.AccountLockStateMapper;
import com.ingot.framework.security.account.adapter.port.DefaultLockStatePortAdapter;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAppAudienceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAudienceDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAudienceGroupMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAudienceMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDefaultPolicyRevisionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDelegationActionCeilingMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDelegationGrantMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDelegationRecipientDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDelegationRecipientMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDelegationRoleRevisionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentWorkbookMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamFieldPolicyMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamFieldRuleMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamIdentityMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberExportMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberWorkbookMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlanApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlanMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformGroupMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformGroupMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPolicySelectorDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPolicySelectorMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPolicySelectorMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamResourceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleDefinitionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleDeltaMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleGrantMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleParameterMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantGroupDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantGroupMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantGroupMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.data.mybatis.config.MybatisPlusConfig;
import com.ingot.framework.data.mybatis.scope.config.DataScopeProperties;
import com.ingot.framework.tenant.properties.TenantProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * <p>
 * 为数据库夹具装配真实 MyBatis Plus/MPJ Mapper，复用生产拦截器并参与 Spring 数据源事务。
 * </p>
 * <p>
 * 测试允许直接 JDBC 准备数据；被测业务通过此处 Mapper 执行，不增加生产 DataSource 便利构造器。 默认枚举处理器与 Nacos 生产配置一致，使用
 * {@code CompositeEnumTypeHandler}。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamMybatisTestAccess {

    private static final Map<DataSource, SqlSessionTemplate> SESSIONS = new ConcurrentHashMap<>();

    private IamMybatisTestAccess() {
    }

    /** 在当前夹具数据源上建立身份 Repository。 */
    public static IdentityRepository identity(DataSource source) {
        return new IdentityRepository(mapper(source, IamIdentityMapper.class));
    }

    /** 在当前夹具数据源上建立组织设置 Repository。 */
    public static TenantRepository tenants(DataSource source) {
        return new TenantRepository(mapper(source, IamTenantMapper.class), mapper(source, IamTenantMemberMapper.class));
    }

    /** 在当前夹具数据源上建立账号与任职查询 Repository，锁定态走安全框架适配器。 */
    public static AccountCredentialRepository accounts(DataSource source) {
        return new AccountCredentialRepository(mapper(source, IamAccountMapper.class), lockStates(source),
                mapper(source, IamTenantMemberMapper.class), mapper(source, IamMemberDepartmentMapper.class));
    }

    /** 在当前夹具数据源上建立安全框架锁定态端口，不使用 IAM 自有锁定实现。 */
    public static LockStatePort lockStates(DataSource source) {
        return new DefaultLockStatePortAdapter(mapper(source, AccountLockStateMapper.class));
    }

    /** 在当前夹具数据源上建立组织初始化目录 Repository。 */
    public static InitializationCatalogRepository catalog(DataSource source) {
        return new InitializationCatalogRepository(mapper(source, IamAccountMapper.class),
                mapper(source, IamRoleDefinitionMapper.class), mapper(source, IamRoleRevisionMapper.class),
                mapper(source, IamDefaultPolicyRevisionMapper.class), mapper(source, IamApplicationMapper.class),
                mapper(source, IamResourceMapper.class),
                mapper(source, IamPlanMapper.class), mapper(source, IamPlanApplicationMapper.class));
    }

    /** 在当前夹具数据源上建立组织初始化写入 Repository。 */
    public static TenantInitializationRepository tenantInit(DataSource source) {
        return new TenantInitializationRepository(mapper(source, IamAccountMapper.class), mapper(source, IamApplicationMapper.class),
                mapper(source, IamDefaultPolicyRevisionMapper.class), mapper(source, IamTenantMapper.class),
                mapper(source, IamTenantMemberMapper.class), mapper(source, IamDepartmentMapper.class),
                mapper(source, IamMemberDepartmentMapper.class),
                mapper(source, IamTenantAppEntitlementMapper.class), mapper(source, IamAppAudienceMapper.class),
                mapper(source, IamFieldPolicyMapper.class), mapper(source, IamResourceMapper.class));
    }

    /** 在当前夹具数据源上建立成员查询与写入 Repository。 */
    public static MemberRepository memberQueries(DataSource source) {
        var queries = new MemberQueryRepository(mapper(source, IamAccountMapper.class), mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class), mapper(source, IamDepartmentMapper.class), mapper(source, IamMemberDepartmentMapper.class));
        return new MemberRepository(queries, new MemberWriteRepository(mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class), mapper(source, IamDepartmentMapper.class), new MemberDepartmentWriteRepository(mapper(source, IamMemberDepartmentMapper.class)), queries));
    }

    /** 在当前夹具数据源上建立冷启动平台身份 Repository。 */
    public static PlatformBootstrapRepository platformBootstrap(DataSource source) {
        return new PlatformBootstrapRepository(mapper(source, IamAccountMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamRoleAssignmentMapper.class));
    }

    /** 在当前夹具数据源上建立账号查询 Repository。 */
    public static AccountQueryRepository accountQueries(DataSource source) {
        return new AccountQueryRepository(mapper(source, IamAccountMapper.class));
    }

    /** 在当前夹具数据源上建立账号写入 Repository。 */
    public static AccountWriteRepository accountWrites(DataSource source,
            com.ingot.cloud.iam.domain.identity.InitializationIdAllocator ids) {
        return new AccountWriteRepository(mapper(source, IamAccountMapper.class), ids);
    }

    /** 在当前夹具数据源上建立成员生命周期 Repository。 */
    public static MemberLifecycleRepository members(DataSource source) {
        return new MemberLifecycleRepository(mapper(source, IamTenantMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamDepartmentMapper.class), mapper(source, IamMemberDepartmentMapper.class),
                new MemberDepartmentWriteRepository(mapper(source, IamMemberDepartmentMapper.class)));
    }

    /** 在当前夹具数据源上建立开通与人群 Repository。 */
    public static EntitlementRepository entitlements(DataSource source) {
        return new EntitlementRepository(mapper(source, IamTenantAppEntitlementMapper.class),
                mapper(source, IamAppAudienceMapper.class), mapper(source, IamAudienceMemberMapper.class),
                mapper(source, IamAudienceDepartmentMapper.class), mapper(source, IamAudienceGroupMapper.class),
                mapper(source, IamTenantMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamDepartmentMapper.class), mapper(source, IamTenantGroupMapper.class));
    }

    /** 在当前夹具数据源上建立角色分配 Repository。 */
    public static AssignmentRepository assignments(DataSource source) {
        return new AssignmentRepository(mapper(source, IamRoleAssignmentMapper.class),
                mapper(source, IamDelegationGrantMapper.class), mapper(source, IamDelegationRoleRevisionMapper.class),
                mapper(source, IamDelegationActionCeilingMapper.class),
                mapper(source, IamPlatformGroupMemberMapper.class), mapper(source, IamTenantGroupMemberMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamPlatformGroupMapper.class),
                mapper(source, IamTenantMemberMapper.class), mapper(source, IamTenantGroupMapper.class),
                mapper(source, IamRoleRevisionMapper.class));
    }

    /** 在当前夹具数据源上建立委派 Repository。 */
    public static DelegationRepository delegations(DataSource source) {
        return new DelegationRepository(mapper(source, IamDelegationGrantMapper.class),
                mapper(source, IamDelegationRoleRevisionMapper.class),
                mapper(source, IamDelegationRecipientMemberMapper.class),
                mapper(source, IamDelegationRecipientDepartmentMapper.class),
                mapper(source, IamDelegationActionCeilingMapper.class), mapper(source, IamRoleAssignmentMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamRoleRevisionMapper.class));
    }

    /** 在当前夹具数据源上建立角色目录 Repository。 */
    public static RoleRepository roles(DataSource source) {
        return new RoleRepository(mapper(source, IamRoleDefinitionMapper.class),
                mapper(source, IamRoleRevisionMapper.class), mapper(source, IamRoleGrantMapper.class),
                mapper(source, IamRoleDeltaMapper.class), mapper(source, IamRoleParameterMapper.class),
                mapper(source, IamRoleAssignmentMapper.class), mapper(source, IamActionMapper.class));
    }

    /** 在当前夹具数据源上建立用户组 Repository。 */
    public static GroupRepository groups(DataSource source) {
        return new GroupRepository(mapper(source, IamPlatformGroupMapper.class),
                mapper(source, IamTenantGroupMapper.class), mapper(source, IamPlatformGroupMemberMapper.class),
                mapper(source, IamTenantGroupMemberMapper.class), mapper(source, IamTenantGroupDepartmentMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamDepartmentMapper.class), mapper(source, IamMemberDepartmentMapper.class),
                mapper(source, IamRoleAssignmentMapper.class));
    }

    /** 在当前夹具数据源上建立委派接收人 Repository。 */
    public static DelegationRecipientRepository recipients(DataSource source) {
        return new DelegationRecipientRepository(mapper(source, IamDelegationRecipientMemberMapper.class),
                mapper(source, IamDelegationRecipientDepartmentMapper.class));
    }

    /** 在当前夹具数据源上建立委派派生授权准入组件。 */
    public static com.ingot.cloud.iam.application.delegation.DelegationAdmission delegationAdmission(DataSource source) {
        return new com.ingot.cloud.iam.application.delegation.DelegationAdmission(assignments(source), recipients(source));
    }

    /** 在同一 Spring 数据源事务上建立审计写入器。 */
    public static AuthorizationAuditWriter audits(DataSource source) {
        return new AuthorizationAuditWriter(mapper(source, IamAuthorizationAuditMapper.class));
    }

    /** 在当前夹具数据源上建立成员导出任务 Repository。 */
    public static MemberExportRepository exports(DataSource source) {
        return new MemberExportRepository(mapper(source, IamMemberExportMapper.class));
    }

    /** 在当前夹具数据源上建立下载中心任务存储。 */
    public static IamDownloadTaskStore downloads(DataSource source,
            com.ingot.cloud.iam.domain.identity.InitializationIdAllocator ids) {
        return new IamDownloadTaskStore(mapper(source,
                com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDownloadTaskMapper.class), ids);
    }

    /** 在当前夹具数据源上建立成员表格任务 Repository。 */
    public static MemberWorkbookRepository workbooks(DataSource source) {
        return new MemberWorkbookRepository(mapper(source, IamMemberWorkbookMapper.class));
    }

    /** 在当前夹具数据源上建立部门表格任务 Repository。 */
    public static DepartmentWorkbookRepository departmentWorkbooks(DataSource source) {
        return new DepartmentWorkbookRepository(mapper(source, IamDepartmentWorkbookMapper.class));
    }

    /** 在当前夹具数据源上建立字段策略求值器。 */
    public static FieldAccessEvaluator fields(DataSource source) {
        return com.ingot.cloud.iam.fixture.FieldAccessEvaluatorFixture.create(policies(source), mapper(source, IamMemberDepartmentMapper.class),
                new DepartmentClosure(mapper(source, IamDepartmentMapper.class)), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache(), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.projection(), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.metadata(), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache());
    }

    /** 在当前夹具数据源上建立字段策略 Repository。 */
    public static com.ingot.cloud.iam.infrastructure.persistence.FieldPolicyRepository policies(DataSource source) {
        return new com.ingot.cloud.iam.infrastructure.persistence.FieldPolicyRepository(mapper(source, IamFieldPolicyMapper.class),
                mapper(source, IamFieldRuleMapper.class), mapper(source, IamPolicySelectorMapper.class),
                mapper(source, IamPolicySelectorMemberMapper.class),
                mapper(source, IamPolicySelectorDepartmentMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamDefaultPolicyRevisionMapper.class));
    }

    /** 在当前夹具数据源上建立授权求值 Repository。 */
    public static AuthorizationEvaluationRepository evaluations(DataSource source) {
        return new AuthorizationEvaluationRepository(mapper(source, IamRoleAssignmentMapper.class),
                mapper(source, IamRoleGrantMapper.class), mapper(source, IamRoleDeltaMapper.class),
                mapper(source, IamRoleRevisionMapper.class), mapper(source, IamActionMapper.class),
                entitlements(source), mapper(source, IamDelegationActionCeilingMapper.class));
    }

    /** 在当前夹具数据源上建立无缓存的授权求值器。 */
    public static AuthorizationEvaluator evaluator(DataSource source) {
        return new AuthorizationEvaluator(evaluations(source), null, null, liveEvaluator(source));
    }

    /** 在当前夹具数据源上建立读取指定热缓存的授权求值器。 */
    public static AuthorizationEvaluator evaluator(DataSource source,
            LayeredCache<String, AuthorizationView> cache) {
        return new AuthorizationEvaluator(evaluations(source), new SingletonProvider<>(cache), null, liveEvaluator(source));
    }

    /** 当前角色仓库供租户实时授权验收。 */
    public static TenantRoleDefinitionRepository currentRoles(DataSource source) {
        return new TenantRoleDefinitionRepository(mapper(source,com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper.class),mapper(source,IamRoleDefinitionMapper.class),mapper(source,IamRoleRevisionMapper.class),mapper(source,IamRoleGrantMapper.class),mapper(source,IamRoleParameterMapper.class),mapper(source,IamRoleDeltaMapper.class));
    }
    /** 租户实时求值器，平台测试仍使用原查询路径。 */
    public static com.ingot.cloud.iam.application.authorization.evaluation.TenantAuthorizationEvaluator liveEvaluator(DataSource source) {
        return new com.ingot.cloud.iam.application.authorization.evaluation.TenantAuthorizationEvaluator(mapper(source,com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper.class),currentRoles(source),entitlements(source),new DepartmentClosure(mapper(source,IamDepartmentMapper.class)));
    }
    /** 在当前夹具数据源上建立范围编译器。 */
    public static ObjectScopeCompiler objectScopes(DataSource source) {
        return new ObjectScopeCompiler(new DepartmentClosure(mapper(source, IamDepartmentMapper.class)),
                mapper(source, IamMemberDepartmentMapper.class));
    }

    /** 在当前夹具数据源上建立对象范围检查。 */
    public static com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService resourceAccess(DataSource source) {
        return new com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService(evaluator(source), objectScopes(source),
                memberQueries(source), new DepartmentQueryRepository(mapper(source, IamDepartmentMapper.class),
                        mapper(source, IamMemberDepartmentMapper.class),
                        mapper(source, IamTenantMemberMapper.class)));
    }

    /** 在当前夹具数据源上建立对象展示能力。 */
    public static com.ingot.cloud.iam.application.authorization.evaluation.ObjectCapabilities objectCapabilities(DataSource source) {
        return new com.ingot.cloud.iam.application.authorization.evaluation.ObjectCapabilities(evaluator(source), resourceAccess(source));
    }

    /** 提供固定实例的最小 ObjectProvider，避免为夹具启动容器。 */
    private record SingletonProvider<T>(T instance) implements ObjectProvider<T> {
        @Override
        public T getObject() {
            return instance;
        }

        @Override
        public T getObject(Object... args) {
            return instance;
        }

        @Override
        public T getIfAvailable() {
            return instance;
        }

        @Override
        public T getIfUnique() {
            return instance;
        }
    }

    /** 无注册字段资源的测试执行能力。 */
    public static com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata(DataSource source) {
        return new com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata(mapper(source, IamApplicationMapper.class),
                mapper(source, IamResourceMapper.class), mapper(source, IamActionMapper.class),
                new com.ingot.framework.authorization.ResourceRegistry(java.util.List.of()), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.manifests(), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache());
    }

    /** 无注册字段资源的角色快照服务。 */
    public static com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService roleFields(DataSource source) {
        return new com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService(fieldMetadata(source), roles(source),
                new com.ingot.cloud.iam.infrastructure.persistence.AuthorizationScopeTransportCompiler(
                        new DepartmentClosure(mapper(source, IamDepartmentMapper.class)),
                        mapper(source, IamMemberDepartmentMapper.class)),
                org.mockito.Mockito.mock(com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator.class), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.manifests(), com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport.noCache());
    }

    /** 在当前夹具数据源上建立只读解析用的目录服务。 */
    public static com.ingot.cloud.iam.application.catalog.CatalogFacade catalogService(
            com.ingot.cloud.iam.application.authorization.IamAccess access, DataSource source,
            org.springframework.transaction.PlatformTransactionManager transactions) {
        return com.ingot.cloud.iam.fixture.CatalogFacadeFixture.create(access, audits(source),
                new com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier(event -> {
                }), catalogs(source), new com.ingot.cloud.iam.application.authorization.SensitiveConfirmationGuard(command -> {
                }), fieldMetadata(source), org.mockito.Mockito.mock(com.ingot.cloud.iam.application.asset.IamImageBinding.class), transactions);
    }

    /** 在当前夹具数据源上建立目录 Repository。 */
    public static CatalogRepository catalogs(DataSource source) {
        return new CatalogRepository(
                new CatalogApplicationRepository(mapper(source, IamApplicationMapper.class), mapper(source, IamTenantAppEntitlementMapper.class)),
                new CatalogResourceRepository(mapper(source, IamResourceMapper.class)),
                new CatalogActionRepository(mapper(source, IamActionMapper.class), mapper(source, IamMenuActionMapper.class), mapper(source, IamRoleGrantMapper.class), mapper(source, IamRoleDeltaMapper.class)),
                new CatalogMenuRepository(mapper(source, IamMenuMapper.class), mapper(source, IamMenuActionMapper.class)),
                new CatalogPlanRepository(mapper(source, IamPlanMapper.class), mapper(source, IamPlanApplicationMapper.class)),
                new CatalogCleanupRepository(mapper(source, IamApplicationMapper.class), mapper(source, IamResourceMapper.class), mapper(source, IamActionMapper.class), mapper(source, IamMenuMapper.class), mapper(source, IamMenuActionMapper.class), mapper(source, IamPlanApplicationMapper.class), mapper(source, IamTenantAppEntitlementMapper.class), mapper(source, IamRoleGrantMapper.class), mapper(source, IamRoleDeltaMapper.class), mapper(source, IamAppAudienceMapper.class), mapper(source, IamAudienceMemberMapper.class), mapper(source, IamAudienceGroupMapper.class), mapper(source, IamAudienceDepartmentMapper.class)));
    }

    /** 取得夹具的真实 Mapper；不接受任意 SQL。 */
    public static <T> T mapper(DataSource source, Class<T> type) {
        return SESSIONS.computeIfAbsent(source, IamMybatisTestAccess::create).getMapper(type);
    }

    /** 在夹具创建执行器前安装观察器，用实际 MyBatis 执行记录验证写入成本。 */
    public static void observeSql(DataSource source, org.apache.ibatis.plugin.Interceptor observer) {
        SESSIONS.computeIfAbsent(source, IamMybatisTestAccess::create)
                .getSqlSessionFactory().getConfiguration().addInterceptor(observer);
    }

    private static SqlSessionTemplate create(DataSource source) {
        try {
            var configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setDefaultEnumTypeHandler(CompositeEnumTypeHandler.class);
            var global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig());
            global.setSqlInjector(new MPJSqlInjector());
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setConfiguration(configuration);
            factory.setGlobalConfig(global);
            factory.setTransactionFactory(new SpringManagedTransactionFactory());
            factory.setPlugins(
                    new IamSqlWallTestInterceptor(),
                    new MybatisPlusConfig().mybatisPlusInterceptor(new TenantProperties(), new DataScopeProperties()),
                    new MPJInterceptor());
            factory.setMapperLocations(new ClassPathResource("mapper/IamIdentityMapper.xml"),
                    new ClassPathResource("sdk/mapper/AccountLockStateMapper.xml"));
            factory.afterPropertiesSet();
            SqlSessionFactory sessions = factory.getObject();
            for (Class<?> mapper : new Class<?>[] { IamAccountMapper.class, AccountLockStateMapper.class,
                    com.ingot.framework.security.credential.data.mapper.PasswordHistoryMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper.class, IamTenantMapper.class, IamTenantMemberMapper.class, IamPlatformMemberMapper.class,
                    IamMemberDepartmentMapper.class, IamDepartmentMapper.class, com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentLeaderMapper.class, IamAuthorizationAuditMapper.class,
                    IamApplicationMapper.class, IamRoleDefinitionMapper.class, IamRoleRevisionMapper.class,
                    IamDefaultPolicyRevisionMapper.class, IamPlanMapper.class, IamPlanApplicationMapper.class,
                    IamRoleAssignmentMapper.class, IamTenantAppEntitlementMapper.class, IamAppAudienceMapper.class,
                    IamAudienceMemberMapper.class, IamAudienceDepartmentMapper.class, IamAudienceGroupMapper.class,
                    IamTenantGroupMapper.class, IamFieldPolicyMapper.class,
                    IamActionMapper.class, IamMenuMapper.class, IamMenuActionMapper.class, IamRoleGrantMapper.class,
                    IamFieldRuleMapper.class, IamPolicySelectorMemberMapper.class,
                    IamPolicySelectorDepartmentMapper.class, IamDelegationGrantMapper.class,
                    IamDelegationRoleRevisionMapper.class, IamDelegationRecipientMemberMapper.class,
                    IamDelegationRecipientDepartmentMapper.class, IamDelegationActionCeilingMapper.class,
                    IamPlatformGroupMapper.class, IamPlatformGroupMemberMapper.class, IamTenantGroupMemberMapper.class,
                    IamRoleDeltaMapper.class, IamRoleParameterMapper.class, IamTenantGroupDepartmentMapper.class,
                    IamPolicySelectorMapper.class, IamResourceMapper.class,
                    IamMemberExportMapper.class,
                    IamMemberWorkbookMapper.class,
                    IamDepartmentWorkbookMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDownloadTaskMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleVersionApplicationTaskMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleVersionApplicationItemMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.RoleWorkspaceMapper.class,
                    com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper.class }) {
                sessions.getConfiguration().addMapper(mapper);
            }
            return new SqlSessionTemplate(sessions);
        }
        catch (Exception exception) {
            throw new IllegalStateException("无法装配 IAM MyBatis 测试夹具", exception);
        }
    }

    /** 独立业务单测隔离图片暂存边界，凭证安全由专用测试验证。 */
    public static com.ingot.cloud.iam.application.asset.IamImageBinding images() {
        return org.mockito.Mockito.mock(com.ingot.cloud.iam.application.asset.IamImageBinding.class,
                invocation -> "bind".equals(invocation.getMethod().getName())
                        ? invocation.getArgument(3) : org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation));
    }
}
