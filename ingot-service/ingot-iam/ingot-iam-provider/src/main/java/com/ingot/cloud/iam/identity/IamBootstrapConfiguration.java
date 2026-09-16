package com.ingot.cloud.iam.identity;

import com.ingot.cloud.iam.persistence.InitializationCatalogRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.PlatformBootstrapRepository;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>仅在 {@code ingot.iam.bootstrap.enabled=true} 时装配冷启动能力，默认整段不生效。</p>
 *
 * <p>把服务与启动器都放在条件装配内，常规环境既不创建冷启动 bean，也不绑定其配置，
 * 因此不存在「关闭开关仍持有初始化依赖」的情况。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IamBootstrapProperties.class)
@ConditionalOnProperty(prefix = IamBootstrapProperties.PREFIX, name = IamBootstrapProperties.ENABLED,
        havingValue = "true")
public class IamBootstrapConfiguration {

    /**
     * 装配首个平台身份的建立流程。
     *
     * @param properties 冷启动配置
     * @param bootstrap 平台身份读写
     * @param catalog 治理目录读取
     * @param members 成员写入
     * @param ids 发号器
     * @param registerUser 安全框架注册用例
     * @param initialPassword 初始密码策略
     * @return 冷启动服务
     */
    @Bean
    public PlatformBootstrapService platformBootstrapService(IamBootstrapProperties properties,
                                                            PlatformBootstrapRepository bootstrap,
                                                            InitializationCatalogRepository catalog,
                                                            MemberQueryRepository members,
                                                            InitializationIdAllocator ids,
                                                            RegisterUserUseCase registerUser,
                                                            InitialPasswordService initialPassword) {
        return new PlatformBootstrapService(properties, bootstrap, catalog, members, ids, registerUser,
                initialPassword);
    }

    /**
     * 在启动完成后执行一次冷启动，前提缺失时启动失败。
     *
     * @param bootstrap 冷启动服务
     * @return 启动器
     */
    @Bean
    public ApplicationRunner platformBootstrapRunner(PlatformBootstrapService bootstrap) {
        return args -> bootstrap.initialize();
    }
}
