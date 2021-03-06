package com.ingot.framework.security.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.constants.CookieConstants;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.core.context.RequestContextHolder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

import static com.ingot.framework.core.constants.SecurityConstants.OAUTH2_BASIC_TYPE_WITH_SPACE;
import static com.ingot.framework.core.constants.SecurityConstants.OAUTH2_BEARER_TYPE_WITH_SPACE;


/**
 * <p>Description  : TokenUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/12.</p>
 * <p>Time         : 下午1:05.</p>
 */
@Slf4j
public final class SecurityUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取 Bearer token
     *
     * @param request Http Servlet Request
     * @return jwt
     */
    public static Optional<String> getBearerToken(@NonNull HttpServletRequest request) {
        Enumeration<String> authHeaders = request.getHeaders(HttpHeaders.AUTHORIZATION);
        String bearerToken = null;
        String item;
        while (authHeaders.hasMoreElements()) {
            item = authHeaders.nextElement();
            if (StrUtil.startWithIgnoreCase(item, OAUTH2_BEARER_TYPE_WITH_SPACE)) {
                bearerToken = item;
            }
            log.info(">>> SecurityUtils - getBearerToken authorization while - header={}", item);
        }

        return Optional.ofNullable(bearerToken);
    }

    /**
     * 获取 Bearer token value
     *
     * @param authorization Header Authorization
     * @return jwt
     */
    public static String getBearerTokenValue(@NonNull String authorization) {
        if (StrUtil.isEmpty(authorization)) {
            return StrUtil.EMPTY;
        }
        if (!StrUtil.startWithIgnoreCase(authorization, OAUTH2_BEARER_TYPE_WITH_SPACE)) {
            return authorization;
        }
        return StrUtil.subAfter(authorization, OAUTH2_BEARER_TYPE_WITH_SPACE, false);
    }

    /**
     * 获取 Basic Token
     *
     * @param request Http Servlet Request
     * @return {@link Optional}, value start with Basic
     */
    public static Optional<String> getBasicToken(@NonNull HttpServletRequest request) {
        Enumeration<String> authHeaders = request.getHeaders(HttpHeaders.AUTHORIZATION);
        String basicHeader = null;
        String item;
        while (authHeaders.hasMoreElements()) {
            item = authHeaders.nextElement();
            if (StrUtil.startWithIgnoreCase(item, OAUTH2_BASIC_TYPE_WITH_SPACE)) {
                basicHeader = item;
            }
            log.info(">>> SecurityUtils - getBasicToken authorization while - header={}", item);
        }

        return Optional.ofNullable(basicHeader);
    }

    /**
     * 获取 token auth 类型
     */
    public static String getAuthType(OAuth2AccessToken accessToken) {
        Map<String, Object> info = accessToken.getAdditionalInformation();
        return ObjectUtil.toString(info.get(SecurityConstants.TokenEnhancer.KEY_FIELD_AUTH_TYPE));
    }

    /**
     * 获取 JWT ID
     *
     * @param accessToken OAuth2AccessToken
     * @return JTI
     */
    public static String getJTI(OAuth2AccessToken accessToken) {
        return getTokenAdditionalInfoByKey(accessToken,
                SecurityConstants.TokenEnhancer.KEY_JTI);
    }

    /**
     * 获取 token additional 信息
     */
    @SuppressWarnings("unchecked")
    public static <T> T getTokenAdditionalInfoByKey(OAuth2AccessToken accessToken, String key) {
        Map<String, Object> info = accessToken.getAdditionalInformation();
        return (T) info.get(key);
    }

    /**
     * 生成 Basic Token，不带 Basic 前缀
     *
     * @param clientId     clientId
     * @param clientSecret clientSecret
     * @return Basic Token
     */
    public static String makeBasicToken(@NonNull String clientId, @NonNull String clientSecret) {
        byte[] token = Base64.getEncoder()
                .encode((clientId + ":" + clientSecret).getBytes());
        return StrUtil.str(token, "");
    }

    /**
     * 解析 Basic Token 中的 client id 和 client secret
     */
    public static String[] extractAndDecodeBasicToken(String basic) throws IOException {
        byte[] base64Token = basic.substring(6).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }

        String token = new String(decoded, "UTF-8");

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[]{token.substring(0, delim), token.substring(delim + 1)};
    }

    /**
     * 获取 cookie 中的访问 token
     */
    public static String getAccessTokenFromCookie(HttpServletRequest request) {
        String tokenObj = CookieUtils.getCookieValue(request, CookieConstants.COOKIES_ADMIN_TOKEN_KEY);
        if (!StrUtil.isEmpty(tokenObj)) {
            try {
                log.info(">>> getAccessTokenFromCookie, HttpServletRequest - {}", tokenObj);
                return objectMapper.readValue(URLDecoder.decode(tokenObj, "UTF-8"),
                        new TypeReference<Map<String, String>>() {
                        }).get("access_token");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取 cookie 中的访问 token
     */
    public static String getAccessTokenFromCookie(ServerHttpRequest request) {
        String tokenObj = CookieUtils.getCookieFirstValue(request, CookieConstants.COOKIES_ADMIN_TOKEN_KEY);
        if (!StrUtil.isEmpty(tokenObj)) {
            try {
                log.info(">>> getAccessTokenFromCookie, ServerHttpRequest - {}", tokenObj);
                return objectMapper.readValue(URLDecoder.decode(tokenObj, "UTF-8"),
                        new TypeReference<Map<String, String>>() {
                        }).get("access_token");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 从当前 servlet request 中获取 Client id
     *
     * @return {@link String}
     */
    public static String getClientIdFromRequest() {
        try {
            HttpServletRequest request = RequestContextHolder.getRequest().orElse(null);
            if (request == null) {
                return "";
            }
            String clientId = null;

            // 尝试从 session 中获取
            HttpSessionRequestCache cache = new HttpSessionRequestCache();
            SavedRequest savedRequest = cache.getRequest(request, null);
            if (savedRequest instanceof DefaultSavedRequest) {
                Map<String, String> query = HttpUtil.decodeParamMap(((DefaultSavedRequest) savedRequest).getQueryString(), CharsetUtil.UTF_8);
                clientId = query.get("client_id");
            }

            if (StrUtil.isNotEmpty(clientId)) {
                return clientId;
            }

            // 尝试从请求头 Authorization 中获取
            String basicToken = getBasicToken(request).orElse("");
            clientId = extractAndDecodeBasicToken(basicToken)[0];
            return clientId;
        } catch (Exception e) {
            log.info(">>> SecurityUtils getClientIdFromRequest 获取 client id 失败", e);
            return "";
        }
    }
}
