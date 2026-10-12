package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.security.CredentialExpirationReason;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.security.core.userdetails.CredentialExpirationMessageResolver;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.core.userdetails.InUserDetailsChecker;
import com.ingot.framework.security.core.userdetails.InUserMetaKeys;
import com.ingot.framework.security.core.userdetails.LoginFailureMessageProperties;
import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.oauth2.core.InOAuth2AuthProperties;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration.LoginFailureMessagesAutoConfiguration;
import com.ingot.framework.security.oauth2.server.authorization.settings.ExtConfigurationSettingNames;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>通过生产认证 configurer 验证默认／自定义 checker 装配及实际认证失败的 HTTP 文案。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class LoginFailureMessagesAuthenticationTest {
    private static final AuthorizationGrantType PASSWORD_GRANT = new AuthorizationGrantType("password");
    private static final String CUSTOM_INITIAL_MESSAGE = "初始密码失效，请联系服务台重置";
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LoginFailureMessagesAutoConfiguration.class));

    @Test
    void resourceOnlyContextDoesNotRegisterAuthenticationChecker() {
        runner.run(context -> {
            assertTrue(context.getBeansOfType(UserDetailsChecker.class).isEmpty());
            assertTrue(context.getBeansOfType(CredentialExpirationMessageResolver.class).isEmpty());
        });
    }

    @Test
    void missingMessageFileUsesBuiltInDefault() {
        runner.withBean(InOAuth2AuthProperties.class).run(context -> {
            UserDetailsChecker checker = context.getBean(UserDetailsChecker.class);
            assertTrue(checker instanceof InUserDetailsChecker);
            assertEquals("初始密码或重置后的密码已过期，请联系管理员重置密码后再登录。",
                    assertThrows(CredentialsExpiredException.class, () -> checker.check(expiredUser(null))).getMessage());
        });
    }

    @Test
    void productionConfigurerUsesConfiguredCheckerAndHttpResponseForBothDomains() {
        AtomicReference<InUser> user = new AtomicReference<>(expiredUser(null));
        runner.withBean(InOAuth2AuthProperties.class)
                .withBean(OAuth2UserDetailsServiceManager.class, () -> authentication -> user.get())
                .withPropertyValues(LoginFailureMessageProperties.PREFIX + ".initial-password-expired=" + CUSTOM_INITIAL_MESSAGE)
                .run(context -> {
                    OAuth2UserDetailsAuthenticationProvider provider = configuredProvider(context);
                    RegisteredClient client = client();
                    OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
                            client, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
                    for (boolean platform : List.of(true, false)) {
                        user.set(expiredUser(platform ? null : 10L));
                        var token = OAuth2UserDetailsAuthenticationToken.unauthenticated(
                                "user", "password", PASSWORD_GRANT, clientPrincipal);
                        CredentialsExpiredException exception = assertThrows(CredentialsExpiredException.class,
                                () -> provider.authenticate(token));
                        assertEquals(CUSTOM_INITIAL_MESSAGE, exception.getMessage());

                        MockHttpServletResponse response = new MockHttpServletResponse();
                        new DefaultAuthenticationFailureHandler().onAuthenticationFailure(
                                new MockHttpServletRequest(), response, exception);
                        var body = new ObjectMapper().readTree(response.getContentAsString(StandardCharsets.UTF_8));
                        assertEquals(400, response.getStatus());
                        assertEquals(BaseErrorCode.BAD_REQUEST.getCode(), body.get("code").asText());
                        assertEquals(CUSTOM_INITIAL_MESSAGE, body.get("message").asText());
                    }
                });
    }

    @Test
    void userProvidedCheckerOverridesDefaultInProductionConfigurer() {
        UserDetailsChecker custom = user -> { throw new DisabledException("自定义检查器"); };
        runner.withBean(InOAuth2AuthProperties.class)
                .withBean(UserDetailsChecker.class, () -> custom)
                .withBean(OAuth2UserDetailsServiceManager.class, () -> authentication -> expiredUser(null))
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(UserDetailsChecker.class).size());
                    assertSame(custom, context.getBean(UserDetailsChecker.class));
                    var clientPrincipal = new OAuth2ClientAuthenticationToken(
                            client(), ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
                    var token = OAuth2UserDetailsAuthenticationToken.unauthenticated(
                            "user", "password", PASSWORD_GRANT, clientPrincipal);
                    assertEquals("自定义检查器", assertThrows(DisabledException.class,
                            () -> configuredProvider(context).authenticate(token)).getMessage());
                });
    }

    private OAuth2UserDetailsAuthenticationProvider configuredProvider(ApplicationContext context) {
        HttpSecurity http = mock(HttpSecurity.class);
        when(http.getSharedObject(ApplicationContext.class)).thenReturn(context);
        when(http.getSharedObject(AuthorizationServerSettings.class)).thenReturn(AuthorizationServerSettings.builder().build());
        when(http.getSharedObject(AuthenticationManagerBuilder.class)).thenReturn(mock(AuthenticationManagerBuilder.class));
        when(http.getSharedObject(OAuth2AuthorizationService.class)).thenReturn(new InMemoryOAuth2AuthorizationService());
        when(http.getSharedObject(OAuth2TokenGenerator.class)).thenReturn(mock(OAuth2TokenGenerator.class));
        ObjectPostProcessor<Object> postProcessor = new ObjectPostProcessor<>() {
            @Override
            @SuppressWarnings("unchecked")
            public <O> O postProcess(O object) {
                return (O) context.getAutowireCapableBeanFactory().initializeBean(object, object.getClass().getName());
            }
        };
        new OAuth2TokenEndpointEnhanceConfigurer(postProcessor).init(http);
        ArgumentCaptor<AuthenticationProvider> providers = ArgumentCaptor.forClass(AuthenticationProvider.class);
        verify(http, times(3)).authenticationProvider(providers.capture());
        return providers.getAllValues().stream().filter(OAuth2UserDetailsAuthenticationProvider.class::isInstance)
                .map(OAuth2UserDetailsAuthenticationProvider.class::cast).findFirst().orElseThrow();
    }

    private RegisteredClient client() {
        return RegisteredClient.withId("web-id").clientId("web").clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(PASSWORD_GRANT)
                .clientSettings(ClientSettings.builder()
                        .setting(ExtConfigurationSettingNames.CLIENT_STATUS, CommonStatusEnum.ENABLE.getValue()).build())
                .build();
    }

    private InUser expiredUser(Long tenantId) {
        return InUser.userDetails(1L, UserTypeEnum.ADMIN.getValue(), tenantId, "user", "hash",
                true, true, false, true, List.of(),
                Map.of(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON,
                        CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue()), List.of(), Map.of());
    }
}
