package com.ingot.framework.security.common.utils;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.context.RequestContextHolder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
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
        byte[] base64Token = basic.substring(6).getBytes(StandardCharsets.UTF_8);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }

        String token = new String(decoded, StandardCharsets.UTF_8);

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[]{token.substring(0, delim), token.substring(delim + 1)};
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

            // 尝试从请求头 Authorization 中获取
            String basicToken = getBasicToken(request).orElse("");
            return extractAndDecodeBasicToken(basicToken)[0];
        } catch (Exception e) {
            log.info(">>> SecurityUtils getClientIdFromRequest 获取 client id 失败", e);
            return "";
        }
    }
}
