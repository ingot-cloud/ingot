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
import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.cloud.iam.identity.IdentityRepository;
import com.ingot.cloud.iam.persistence.mapper.IamAccountLockStateMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAppAudienceMapper;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDefaultPolicyRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDirectoryPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamIdentityMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMenuActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlanApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlanMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleDefinitionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.data.mybatis.config.MybatisPlusConfig;
import com.ingot.framework.data.mybatis.scope.config.DataScopeProperties;
import com.ingot.framework.tenant.properties.TenantProperties;
import org.apache.ibatis.session.SqlSessionFactory;
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

    /** 在当前夹具数据源上建立账号与任职查询 Repository。 */
    public static AccountCredentialRepository accounts(DataSource source) {
        return new AccountCredentialRepository(mapper(source, IamAccountMapper.class),
                mapper(source, IamAccountLockStateMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamMemberDepartmentMapper.class));
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

    /** 在当前夹具数据源上建立成员生命周期 Repository。 */
    public static MemberLifecycleRepository members(DataSource source) {
        return new MemberLifecycleRepository(mapper(source, IamTenantMapper.class),
                mapper(source, IamPlatformMemberMapper.class), mapper(source, IamTenantMemberMapper.class),
                mapper(source, IamDepartmentMapper.class), mapper(source, IamMemberDepartmentMapper.class));
    }

    /** 在同一 Spring 数据源事务上建立审计写入器。 */
    public static IamAuditWriter audits(DataSource source) {
        return new IamAuditWriter(mapper(source, IamAuthorizationAuditMapper.class));
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
            factory.setMapperLocations(new ClassPathResource("mapper/IamIdentityMapper.xml"));
            factory.afterPropertiesSet();
            SqlSessionFactory sessions = factory.getObject();
            for (Class<?> mapper : new Class<?>[]{
                    IamAccountMapper.class, IamAccountLockStateMapper.class, IamTenantMapper.class,
                    IamTenantMemberMapper.class, IamPlatformMemberMapper.class, IamMemberDepartmentMapper.class,
                    IamDepartmentMapper.class, IamAuthorizationAuditMapper.class, IamApplicationMapper.class,
                    IamRoleDefinitionMapper.class, IamRoleRevisionMapper.class, IamDefaultPolicyRevisionMapper.class,
                    IamPlanMapper.class, IamPlanApplicationMapper.class, IamRoleAssignmentMapper.class,
                    IamTenantAppEntitlementMapper.class, IamAppAudienceMapper.class, IamDirectoryPolicyMapper.class,
                    IamFieldPolicyMapper.class, IamActionMapper.class, IamMenuMapper.class, IamMenuActionMapper.class
            }) {
                sessions.getConfiguration().addMapper(mapper);
            }
            return new SqlSessionTemplate(sessions);
        } catch (Exception exception) {
            throw new IllegalStateException("无法装配 IAM MyBatis 测试夹具", exception);
        }
    }
}
