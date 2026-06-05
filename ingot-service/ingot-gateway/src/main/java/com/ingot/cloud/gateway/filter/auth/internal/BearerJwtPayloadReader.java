package com.ingot.cloud.gateway.filter.auth.internal;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Map;

/**
 * 轻量读取 Bearer JWT payload（不验签，仅用于网关内部身份维度）。
 *
 * <p>用户 ID 取自 claim {@code i}，与 ingot-security {@code JwtClaimNamesExtension.ID} 一致。</p>
 */
@Slf4j
public final class BearerJwtPayloadReader {

    /** 与 {@code JwtClaimNamesExtension.ID} 一致。 */
    public static final String CLAIM_USER_ID = "i";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BearerJwtPayloadReader() {
    }

    /**
     * 从 {@code Authorization: Bearer ...} 值中提取 userId；解析失败返回 null。
     */
    public static String readUserId(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            return null;
        }
        return readUserIdFromJwt(token);
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
            Object raw = claims.get(CLAIM_USER_ID);
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
