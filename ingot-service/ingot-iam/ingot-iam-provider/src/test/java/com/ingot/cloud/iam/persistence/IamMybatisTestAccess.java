package com.ingot.cloud.iam.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.github.yulichang.injector.MPJSqlInjector;
import com.github.yulichang.interceptor.MPJInterceptor;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.evaluation.ObjectScopeCompiler;
import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.cloud.iam.identity.IdentityRepository;
import com.ingot.framework.security.account.adapter.mapper.AccountLockStateMapper;
import com.ingot.framework.security.account.adapter.port.DefaultLockStatePortAdapter;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAppAudienceMapper;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAudienceDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAudienceGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAudienceMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDefaultPolicyRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationActionCeilingMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationGrantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRecipientDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRecipientMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDirectoryPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDirectoryRuleMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldRuleMapper;
import com.ingot.cloud.iam.persistence.mapper.IamIdentityMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMenuActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberExportMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlanApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlanMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformGroupMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamResourceMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleDefinitionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleDeltaMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleGrantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleParameterMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAuditWriter;
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
 * <p>为数据库夹具装配真实 MyBatis Plus/MPJ Mapper，复用生产拦截器并参与 Spring 数据源事务。</p>
 * <p>测试允许直接 JDBC 准备数据；被测业务通过此处 Mapper 执行，不增加生产 DataSource 便利构造器。
 * 默认枚举处理器与 Nacos 生产配置一致，使用 {@code CompositeEnumTypeHandler}。</p>
 * @author jy
 * @since 1.0.0
 */
public final class IamMybatisTestAccess {
    private static final Map<DataSource, SqlSessionTemplate> SESSIONS = new ConcurrentHashMap<>();
    private IamMybatisTestAccess() { }

    /** 在当前夹具数据源上建立身份 Repository。 */
    public static IdentityRepository identity(DataSource source) {
        return new IdentityRepository(mapper(source, IamIdentityMapper.class));
    }

    /** 在当前夹具数据源上建立组织设置 Repository。 */
    public static TenantRepository tenants(DataSource source) {
        return new TenantRepository(mapper(source, IamTenantMapper.class),
                mapper(source, IamTenantMemberMapper.class));
    }

    /** 在当前夹具数据源上建立账号与任职查询 Repository，锁定态走安全框架适配器。 */
    public static AccountCredentialRepository accounts(DataSource source) {
        return new AccountCredentialRepository(mapper(source, IamAccountMapper.class),
                lockStates(source), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamMemberDepartmentMapper.class));
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
                mapper(source, IamPlanMapper.class), mapper(source, IamPlanApplicationMapper.class));
    }

    /** 在当前夹具数据源上建立组织初始化写入 Repository。 */
    public static TenantInitializationRepository tenantInit(DataSource source) {
        return new TenantInitializationRepository(mapper(source, IamAccountMapper.class),
                mapper(source, IamRoleRevisionMapper.class), mapper(source, IamApplicationMapper.class),
                mapper(source, IamDefaultPolicyRevisionMapper.class), mapper(source, IamTenantMapper.class),
                mapper(source, IamTenantMemberMapper.class), mapper(source, IamDepartmentMapper.class),
                mapper(source, IamMemberDepartmentMapper.class), mapper(source, IamRoleAssignmentMapper.class),
                mapper(source, IamTenantAppEntitlementMapper.class), mapper(source, IamAppAudienceMapper.class),
                mapper(source, IamDirectoryPolicyMapper.class), mapper(source, IamFieldPolicyMapper.class));
    }

    /** 在当前夹具数据源上建立成员查询与写入 Repository。 */
    public static MemberQueryRepository memberQueries(DataSource source) {
        return new MemberQueryRepository(mapper(source, IamAccountMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamDepartmentMapper.class), mapper(source, IamMemberDepartmentMapper.class));
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
                                                      com.ingot.cloud.iam.identity.InitializationIdAllocator ids) {
        return new AccountWriteRepository(mapper(source, IamAccountMapper.class), ids);
    }

    /** 在当前夹具数据源上建立成员生命周期 Repository。 */
    public static MemberLifecycleRepository members(DataSource source) {
        return new MemberLifecycleRepository(mapper(source, IamTenantMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamDepartmentMapper.class), mapper(source, IamMemberDepartmentMapper.class));
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
    public static com.ingot.cloud.iam.delegation.DelegationAdmission delegationAdmission(DataSource source) {
        return new com.ingot.cloud.iam.delegation.DelegationAdmission(assignments(source), recipients(source));
    }

    /** 在同一 Spring 数据源事务上建立审计写入器。 */
    public static IamAuditWriter audits(DataSource source) {
        return new IamAuditWriter(mapper(source, IamAuthorizationAuditMapper.class));
    }

    /** 在当前夹具数据源上建立成员导出任务 Repository。 */
    public static MemberExportRepository exports(DataSource source) {
        return new MemberExportRepository(mapper(source, IamMemberExportMapper.class));
    }

    /** 在当前夹具数据源上建立字段策略求值器。 */
    public static FieldAccessEvaluator fields(DataSource source) {
        return new FieldAccessEvaluator(policies(source), mapper(source, IamMemberDepartmentMapper.class),
                new DepartmentClosure(mapper(source, IamDepartmentMapper.class)));
    }

    /** 在当前夹具数据源上建立通讯录与字段策略 Repository。 */
    public static com.ingot.cloud.iam.policy.PolicyWriteRepository policies(DataSource source) {
        return new com.ingot.cloud.iam.policy.PolicyWriteRepository(
                mapper(source, IamDirectoryPolicyMapper.class), mapper(source, IamFieldPolicyMapper.class),
                mapper(source, IamDirectoryRuleMapper.class), mapper(source, IamFieldRuleMapper.class),
                mapper(source, IamPolicySelectorMapper.class), mapper(source, IamPolicySelectorMemberMapper.class),
                mapper(source, IamPolicySelectorDepartmentMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamMemberDepartmentMapper.class), mapper(source, IamDepartmentMapper.class),
                mapper(source, IamDefaultPolicyRevisionMapper.class));
    }

    /** 在当前夹具数据源上建立通讯录可见性求值器。 */
    public static com.ingot.cloud.iam.policy.DirectoryVisibilityEvaluator directory(DataSource source) {
        return new com.ingot.cloud.iam.policy.DirectoryVisibilityEvaluator(policies(source),
                new DepartmentClosure(mapper(source, IamDepartmentMapper.class)));
    }

    /** 在当前夹具数据源上建立授权求值 Repository。 */
    public static AuthorizationEvaluationRepository evaluations(DataSource source) {
        return new AuthorizationEvaluationRepository(mapper(source, IamRoleAssignmentMapper.class),
                mapper(source, IamRoleGrantMapper.class), mapper(source, IamRoleDeltaMapper.class),
                mapper(source, IamRoleRevisionMapper.class), mapper(source, IamActionMapper.class),
                mapper(source, IamTenantAppEntitlementMapper.class),
                mapper(source, IamDelegationActionCeilingMapper.class));
    }

    /** 在当前夹具数据源上建立无缓存的授权求值器。 */
    public static AuthorizationEvaluator evaluator(DataSource source) {
        return new AuthorizationEvaluator(evaluations(source), null, null);
    }

    /** 在当前夹具数据源上建立读取指定热缓存的授权求值器。 */
    public static AuthorizationEvaluator evaluator(
            DataSource source,
            LayeredCache<String, AuthorizationEvaluator.AuthorizationView> cache) {
        return new AuthorizationEvaluator(evaluations(source), new SingletonProvider<>(cache), null);
    }

    /** 在当前夹具数据源上建立范围编译器。 */
    public static ObjectScopeCompiler objectScopes(DataSource source) {
        return new ObjectScopeCompiler(new DepartmentClosure(mapper(source, IamDepartmentMapper.class)),
                mapper(source, IamMemberDepartmentMapper.class));
    }

    /** 在当前夹具数据源上建立对象范围检查。 */
    public static com.ingot.cloud.iam.evaluation.ResourceAccess resourceAccess(DataSource source) {
        return new com.ingot.cloud.iam.evaluation.ResourceAccess(evaluator(source), objectScopes(source),
                memberQueries(source), new DepartmentQueryRepository(mapper(source, IamDepartmentMapper.class),
                mapper(source, IamMemberDepartmentMapper.class)));
    }

    /** 在当前夹具数据源上建立对象展示能力。 */
    public static com.ingot.cloud.iam.evaluation.ObjectCapabilities objectCapabilities(DataSource source) {
        return new com.ingot.cloud.iam.evaluation.ObjectCapabilities(evaluator(source), resourceAccess(source));
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

    /** 在当前夹具数据源上建立目录 Repository。 */
    public static CatalogRepository catalogs(DataSource source) {
        return new CatalogRepository(mapper(source, IamApplicationMapper.class),
                mapper(source, IamResourceMapper.class), mapper(source, IamActionMapper.class),
                mapper(source, IamMenuMapper.class), mapper(source, IamMenuActionMapper.class),
                mapper(source, IamPlanMapper.class), mapper(source, IamPlanApplicationMapper.class),
                mapper(source, IamTenantAppEntitlementMapper.class), mapper(source, IamRoleGrantMapper.class),
                mapper(source, IamRoleDeltaMapper.class));
    }

    /** 取得夹具的真实 Mapper；不接受任意 SQL。 */
    public static <T> T mapper(DataSource source, Class<T> type) {
        return SESSIONS.computeIfAbsent(source, IamMybatisTestAccess::create).getMapper(type);
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
            factory.setPlugins(new MybatisPlusConfig().mybatisPlusInterceptor(new TenantProperties(), new DataScopeProperties()),
                    new MPJInterceptor());
            factory.setMapperLocations(new ClassPathResource("mapper/IamIdentityMapper.xml"),
                    new ClassPathResource("sdk/mapper/AccountLockStateMapper.xml"));
            factory.afterPropertiesSet();
            SqlSessionFactory sessions = factory.getObject();
            for (Class<?> mapper : new Class<?>[]{
                    IamAccountMapper.class, AccountLockStateMapper.class, IamTenantMapper.class,
                    IamTenantMemberMapper.class, IamPlatformMemberMapper.class, IamMemberDepartmentMapper.class,
                    IamDepartmentMapper.class, IamAuthorizationAuditMapper.class, IamApplicationMapper.class,
                    IamRoleDefinitionMapper.class, IamRoleRevisionMapper.class, IamDefaultPolicyRevisionMapper.class,
                    IamPlanMapper.class, IamPlanApplicationMapper.class, IamRoleAssignmentMapper.class,
                    IamTenantAppEntitlementMapper.class, IamAppAudienceMapper.class, IamAudienceMemberMapper.class,
                    IamAudienceDepartmentMapper.class, IamAudienceGroupMapper.class, IamTenantGroupMapper.class,
                    IamDirectoryPolicyMapper.class, IamFieldPolicyMapper.class, IamActionMapper.class,
                    IamMenuMapper.class, IamMenuActionMapper.class, IamRoleGrantMapper.class, IamFieldRuleMapper.class,
                    IamPolicySelectorMemberMapper.class, IamPolicySelectorDepartmentMapper.class,
                    IamDelegationGrantMapper.class, IamDelegationRoleRevisionMapper.class,
                    IamDelegationRecipientMemberMapper.class, IamDelegationRecipientDepartmentMapper.class,
                    IamDelegationActionCeilingMapper.class, IamPlatformGroupMapper.class,
                    IamPlatformGroupMemberMapper.class, IamTenantGroupMemberMapper.class,
                    IamRoleDeltaMapper.class, IamRoleParameterMapper.class, IamTenantGroupDepartmentMapper.class,
                    IamDirectoryRuleMapper.class, IamPolicySelectorMapper.class, IamResourceMapper.class,
                    IamMemberExportMapper.class
            }) {
                sessions.getConfiguration().addMapper(mapper);
            }
            return new SqlSessionTemplate(sessions);
        } catch (Exception exception) {
            throw new IllegalStateException("无法装配 IAM MyBatis 测试夹具", exception);
        }
    }
}
