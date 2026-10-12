package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.core.InSecurityMessageSource;
import com.ingot.framework.security.core.userdetails.CredentialExpirationMessageResolver;
import com.ingot.framework.security.core.userdetails.InUserDetailsChecker;
import com.ingot.framework.security.oauth2.core.InOAuth2AuthProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * <p>仅为授权服务装配动态密码过期提示与默认账号检查器，保留消费者的自定义覆盖。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnBean(InOAuth2AuthProperties.class)
public class LoginFailureMessagesAutoConfiguration {

    /** 创建每次读取当前 Environment 的文案解析器。 */
    @Bean
    @ConditionalOnMissingBean
    public CredentialExpirationMessageResolver credentialExpirationMessageResolver(Environment environment) {
        return new CredentialExpirationMessageResolver(environment);
    }

    /** 未声明自定义 checker 时，为认证 Provider 提供场景化账号状态检查器。 */
    @Bean
    @ConditionalOnMissingBean(UserDetailsChecker.class)
    public InUserDetailsChecker inUserDetailsChecker(CredentialExpirationMessageResolver expirationMessages) {
        return new InUserDetailsChecker(new InSecurityMessageSource(), expirationMessages);
    }
}
