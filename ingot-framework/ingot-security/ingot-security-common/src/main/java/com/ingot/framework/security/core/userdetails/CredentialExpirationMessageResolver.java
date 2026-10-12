package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.commons.model.security.CredentialExpirationReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * <p>从当前环境解析密码过期提示，配置删除、空白或绑定失败时回退内置消息。</p>
 *
 * <p>每次新绑定配置对象，不持有 Nacos 刷新前的字段快照；文案不参与认证决策。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class CredentialExpirationMessageResolver {
    private static final String INITIAL_MESSAGE_KEY = "InUserDetailsChecker.initialPasswordExpired";
    private static final String PASSWORD_MESSAGE_KEY = "InUserDetailsChecker.passwordExpired";
    private static final String UNKNOWN_MESSAGE_KEY = "AccountStatusUserDetailsChecker.credentialsExpired";
    private static final String DEFAULT_INITIAL_MESSAGE =
            "初始密码或重置后的密码已过期，请联系管理员重置密码后再登录。";
    private static final String DEFAULT_PASSWORD_MESSAGE =
            "密码已过期，当前无法登录，请联系管理员重置密码后再登录。";
    private static final String DEFAULT_UNKNOWN_MESSAGE =
            "登录密码已过期，请联系管理员重置密码后再登录。";

    private final Environment environment;

    /**
     * 按原因读取当前配置，返回自定义纯文本或对应内置提示。
     *
     * @param reason 缺省时按未知原因处理
     * @param messages 认证端当前使用的安全消息资源
     */
    public String resolve(CredentialExpirationReason reason, MessageSourceAccessor messages) {
        LoginFailureMessageProperties properties = currentProperties();
        CredentialExpirationReason effectiveReason = reason == null ? CredentialExpirationReason.UNKNOWN : reason;
        return switch (effectiveReason) {
            case INITIAL_PASSWORD_EXPIRED -> configuredOrDefault(properties.getInitialPasswordExpired(), messages,
                    INITIAL_MESSAGE_KEY, DEFAULT_INITIAL_MESSAGE);
            case PASSWORD_EXPIRED -> configuredOrDefault(properties.getPasswordExpired(), messages,
                    PASSWORD_MESSAGE_KEY, DEFAULT_PASSWORD_MESSAGE);
            case UNKNOWN -> configuredOrDefault(properties.getCredentialsExpired(), messages,
                    UNKNOWN_MESSAGE_KEY, DEFAULT_UNKNOWN_MESSAGE);
        };
    }

    private LoginFailureMessageProperties currentProperties() {
        try {
            return new Binder(ConfigurationPropertySources.get(environment), PlaceholdersResolver.NONE)
                    .bind(LoginFailureMessageProperties.PREFIX, LoginFailureMessageProperties.class)
                    .orElseGet(LoginFailureMessageProperties::new);
        } catch (BindException exception) {
            log.warn("[CredentialExpirationMessage] 文案配置绑定失败，使用内置提示", exception);
            return new LoginFailureMessageProperties();
        }
    }

    private String configuredOrDefault(String configured, MessageSourceAccessor messages,
                                       String messageKey, String fallback) {
        return StringUtils.hasText(configured) ? configured : messages.getMessage(messageKey, fallback);
    }
}
