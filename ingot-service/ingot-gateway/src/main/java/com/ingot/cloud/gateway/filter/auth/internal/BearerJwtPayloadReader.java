package com.ingot.cloud.gateway.filter.auth.internal;

import java.util.Base64;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.constants.InJwtClaimNames;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 轻量读取 Bearer JWT payload 的工具类（不验签）。
 *
 * <p>仅供 {@link com.ingot.cloud.gateway.filter.auth.AuthContextRelayFilter} 提取网关内部
 * 限流 / 黑白名单 {@code USER} 维度；token 真伪与权限由下游 Resource Server 校验。</p>
 *
 * <h3>解析规则</h3>
 * <ul>
 *     <li>从 {@code Authorization: Bearer &lt;token&gt;} 提取 JWT 字符串</li>
 *     <li>Base64URL 解码 payload 段（第二段），不校验 header / signature</li>
 *     <li>claim 名一律取自 {@link InJwtClaimNames}：用户 ID 为 {@code i}，
 *         JWT ID 为 {@code jti}；瘦身前遗留 token 可能仍带 {@code ut}</li>
 * </ul>
 *
 * <p>解析失败（格式错误、claim 缺失等）返回 {@code null}，调用方不阻断请求。</p>
 */
@Slf4j
@UtilityClass
public class BearerJwtPayloadReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 从 {@code Authorization: Bearer ...} 值中提取 userId；解析失败返回 null。
     */
    public static String readUserId(String authorizationHeader) {
        return readClaimFromAuthorization(authorizationHeader, InJwtClaimNames.ID);
    }

    /**
     * 从 {@code Authorization: Bearer ...} 值中提取 jti；解析失败返回 null。
     */
    public static String readJti(String authorizationHeader) {
        return readClaimFromAuthorization(authorizationHeader, InJwtClaimNames.JTI);
    }

    /**
     * 从 {@code Authorization: Bearer ...} 值中提取 userType（claim {@code ut}）。
     * <p>瘦身 JWT 通常不含该 claim，仅作为遗留 token 降级路径。</p>
     */
    public static String readUserType(String authorizationHeader) {
        return readClaimFromAuthorization(authorizationHeader, InJwtClaimNames.USER_TYPE);
    }

    static String extractBearerToken(String authorizationHeader) {
        if (StrUtil.isBlank(authorizationHeader)) {
            return null;
        }
        String prefix = "Bearer ";
        if (!StrUtil.startWithIgnoreCase(authorizationHeader, prefix)) {
            return null;
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    static String readUserIdFromJwt(String jwt) {
        return readClaimAsString(jwt, InJwtClaimNames.ID);
    }

    private static String readClaimFromAuthorization(String authorizationHeader, String claimName) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            return null;
        }
        return readClaimAsString(token, claimName);
    }

    static String readClaimAsString(String jwt, String claimName) {
        int firstDot = jwt.indexOf('.');
        if (firstDot < 0) {
            return null;
        }
        int secondDot = jwt.indexOf('.', firstDot + 1);
        if (secondDot < 0) {
            return null;
        }
        String payloadSegment = jwt.substring(firstDot + 1, secondDot);
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadSegment);
            Map<String, Object> claims = MAPPER.readValue(decoded, new TypeReference<>() {
            });
            Object raw = claims.get(claimName);
            if (raw == null) {
                return null;
            }
            if (raw instanceof Number number) {
                return String.valueOf(number.longValue());
            }
            String text = String.valueOf(raw).trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.debug("[BearerJwtPayloadReader] parse jwt payload failed", e);
            return null;
        }
    }
}
