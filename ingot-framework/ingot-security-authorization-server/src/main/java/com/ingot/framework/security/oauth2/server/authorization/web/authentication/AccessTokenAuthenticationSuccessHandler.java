package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.commons.model.common.AuthSuccessDTO;
import com.ingot.framework.commons.model.event.LoginEvent;
import com.ingot.framework.commons.utils.CookieUtil;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.WebUtil;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.security.oauth2.server.authorization.common.OAuth2Util;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.CustomOAuth2AccessTokenResponseHttpMessageConverter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

/**
 * <p>Description  : AccessTokenAuthenticationSuccessHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:48 上午.</p>
 */
@Slf4j
public class AccessTokenAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenHttpResponseConverter =
            new CustomOAuth2AccessTokenResponseHttpMessageConverter();

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


        String sessionId = Optional.ofNullable(additionalParameters.get(InOAuth2ParameterNames.SESSION_ID))
                .map(String::valueOf)
                .orElse("");
        if (StrUtil.isNotEmpty(sessionId)) {
            CookieUtil.setCookie(CookieUtil.SESSION_ID_NAME, sessionId, null, true, false, response);
            additionalParameters.remove(InOAuth2ParameterNames.SESSION_ID);
        }

        long expiresIn = accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null ?
                ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()) : -1;
        OAuth2AccessTokenResponse.Builder builder =
                OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                        .tokenType(accessToken.getTokenType())
                        .scopes(accessToken.getScopes())
                        .expiresIn(expiresIn);
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
        OAuth2AccessTokenAuthenticationToken accessTokenAuthentication =
                (OAuth2AccessTokenAuthenticationToken) authentication;
        Map<String, Object> additionalParameters = accessTokenAuthentication.getAdditionalParameters();

        String username = OAuth2Util.getLoginUsername(additionalParameters);
        String org = OAuth2Util.getLoginOrg(additionalParameters);
        // 清理多余参数
        OAuth2Util.clearLoginInfo(additionalParameters);

        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);

        AuthSuccessDTO payload = new AuthSuccessDTO();
        payload.setGrantType(grantType);
        payload.setIp(WebUtil.getClientIP(request));
        payload.setUsername(username);
        payload.setOrg(org);
        payload.setTime(DateUtil.now());
        SpringContextHolder.publishEvent(new LoginEvent(payload));
    }


}
