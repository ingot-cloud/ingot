package com.ingot.cloud.gateway.filter.auth.internal;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Map;

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
 *     <li>用户 ID 取自 claim {@link #CLAIM_USER_ID}（{@code i}），与 ingot-security
 *         {@code JwtClaimNamesExtension.ID} 一致；支持数值与字符串类型</li>
 * </ul>
 *
 * <p>解析失败（格式错误、claim 缺失等）返回 {@code null}，调用方不阻断请求。</p>
 */
@Slf4j
@UtilityClass
public class BearerJwtPayloadReader {

    /** JWT payload 中用户 ID 的 claim 名；与 {@code JwtClaimNamesExtension.ID} 一致。 */
    public static final String CLAIM_USER_ID = "i";

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
