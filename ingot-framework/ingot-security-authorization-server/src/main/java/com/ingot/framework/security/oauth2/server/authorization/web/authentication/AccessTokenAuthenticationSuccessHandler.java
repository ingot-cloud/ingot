package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.extra.servlet.ServletUtil;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.model.dto.common.AuthSuccessDTO;
import com.ingot.framework.core.model.event.LoginEvent;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.IngotOAuth2AccessTokenResponseHttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * <p>Description  : AccessTokenAuthenticationSuccessHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:48 上午.</p>
 */
@Slf4j
public class AccessTokenAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenHttpResponseConverter =
            new IngotOAuth2AccessTokenResponseHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("[AccessTokenAuthenticationSuccessHandler] - onAuthenticationSuccess authentication={}",
                authentication);
        this.sendSuccessEventLog(request, authentication);
        this.sendAccessTokenResponse(response, authentication);
    }

    private void sendAccessTokenResponse(HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2AccessTokenAuthenticationToken accessTokenAuthentication =
                (OAuth2AccessTokenAuthenticationToken) authentication;

        OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
        OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();
        Map<String, Object> additionalParameters = accessTokenAuthentication.getAdditionalParameters();

        OAuth2AccessTokenResponse.Builder builder =
                OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                        .tokenType(accessToken.getTokenType())
                        .scopes(accessToken.getScopes());
        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            builder.expiresIn(ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        }
        if (refreshToken != null) {
            builder.refreshToken(refreshToken.getTokenValue());
        }
        if (!CollectionUtils.isEmpty(additionalParameters)) {
            builder.additionalParameters(additionalParameters);
        }
        OAuth2AccessTokenResponse accessTokenResponse = builder.build();
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        this.accessTokenHttpResponseConverter.write(accessTokenResponse, null, httpResponse);
    }

    private void sendSuccessEventLog(HttpServletRequest request,
                                     Authentication authentication) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);

        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);
        AuthSuccessDTO payload = new AuthSuccessDTO();
        payload.setGrantType(grantType);
        payload.setIp(ServletUtil.getClientIP(request));
        payload.setUsername(parameters.getFirst(OAuth2ParameterNames.USERNAME));
        payload.setTime(DateUtils.now());
        SpringContextHolder.publishEvent(new LoginEvent(payload));
    }
}
