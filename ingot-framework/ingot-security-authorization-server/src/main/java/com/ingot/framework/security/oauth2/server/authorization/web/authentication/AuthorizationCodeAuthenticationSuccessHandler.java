package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import com.ingot.framework.core.constants.InOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.OAuth2AuthorizationCodeRequestHttpMessageConverter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <p>Description  : AuthorizationCodeAuthenticationSuccessHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/9.</p>
 * <p>Time         : 4:01 PM.</p>
 */
public class AuthorizationCodeAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final HttpMessageConverter<OAuth2AuthorizationCodeRequestAuthenticationToken> responseConverter =
            new OAuth2AuthorizationCodeRequestHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);
        // 如果包含pre_grant_type，代表是预授权过来的
        if (parameters.containsKey(InOAuth2ParameterNames.PRE_GRANT_TYPE)) {
            sendResponse(request, response, authentication);
            return;
        }

        defaultRedirect(request, response, authentication);
    }

    private void sendResponse(HttpServletRequest request,
                              HttpServletResponse response,
                              Authentication authentication) throws IOException {
        OAuth2AuthorizationCodeRequestAuthenticationToken token =
                (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;

        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        this.responseConverter.write(token, null, httpResponse);
    }

    private void defaultRedirect(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Authentication authentication) throws IOException {
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
                (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(authorizationCodeRequestAuthentication.getRedirectUri())
                .queryParam(OAuth2ParameterNames.CODE, authorizationCodeRequestAuthentication.getAuthorizationCode().getTokenValue());
        if (StringUtils.hasText(authorizationCodeRequestAuthentication.getState())) {
            uriBuilder.queryParam(
                    OAuth2ParameterNames.STATE,
                    UriUtils.encode(authorizationCodeRequestAuthentication.getState(), StandardCharsets.UTF_8));
        }
        String redirectUri = uriBuilder.build(true).toUriString();        // build(true) -> Components are explicitly encoded
        this.redirectStrategy.sendRedirect(request, response, redirectUri);
    }
}
