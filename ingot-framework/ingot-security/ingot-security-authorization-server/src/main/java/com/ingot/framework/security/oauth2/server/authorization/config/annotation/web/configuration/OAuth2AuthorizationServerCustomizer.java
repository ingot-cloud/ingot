package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.authentication.AuthenticatedMemberBinder;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.AuthorizationCodeAuthenticationSuccessHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.CustomOAuth2AuthorizationCodeRequestAuthenticationConverter;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationEndpointConfigurer;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;

/**
 * <p>替换授权码请求转换器，并在租户选定后绑定 IAM 成员上下文。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
public class OAuth2AuthorizationServerCustomizer implements Customizer<OAuth2AuthorizationEndpointConfigurer> {

    private final AuthenticatedMemberBinder memberBinder;

    /**
     * 不绑定成员，仅替换授权码转换器。
     */
    public OAuth2AuthorizationServerCustomizer() {
        this(null);
    }

    /**
     * 使用成员绑定器替换授权码请求转换器。
     *
     * @param memberBinder 租户选定后绑定成员；为空则推迟到签发阶段
     */
    public OAuth2AuthorizationServerCustomizer(AuthenticatedMemberBinder memberBinder) {
        this.memberBinder = memberBinder;
    }

    @Override
    public void customize(OAuth2AuthorizationEndpointConfigurer configurer) {
        // remove OAuth2AuthorizationCodeRequestAuthenticationConverter
        // add CustomOAuth2AuthorizationCodeRequestAuthenticationConverter
        // 替换原有Converter
        configurer.authorizationRequestConverters(converters -> {
            converters.removeIf(converter -> converter instanceof OAuth2AuthorizationCodeRequestAuthenticationConverter);
            converters.add(new CustomOAuth2AuthorizationCodeRequestAuthenticationConverter(memberBinder));
        });
        configurer.authorizationResponseHandler(new AuthorizationCodeAuthenticationSuccessHandler());
        configurer.errorResponseHandler(new DefaultAuthenticationFailureHandler());
    }
}
