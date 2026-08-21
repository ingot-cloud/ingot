package com.ingot.cloud.gateway.filter.auth.internal;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.InJwtClaimNames;
import com.ingot.framework.commons.utils.JwtPayloadUtil;
import lombok.experimental.UtilityClass;

/**
 * 从 {@code Authorization: Bearer ...} 请求头轻量读取 JWT 声明（不验签）。
 *
 * <p>仅供 {@link com.ingot.cloud.gateway.filter.auth.AuthContextRelayFilter} 提取网关内部
 * 限流 / 黑白名单 {@code USER} 维度与会话定位；token 真伪与权限由下游 Resource Server 校验。</p>
 *
 * <h3>解析规则</h3>
 * <ul>
 *     <li>从 {@code Authorization: Bearer &lt;token&gt;} 提取 JWT 字符串</li>
 *     <li>payload 解析委托 {@link JwtPayloadUtil}，不校验 header / signature</li>
 *     <li>claim 名一律取自 {@link InJwtClaimNames}：用户 ID 为 {@code i}，会话 ID 为 {@code sid}</li>
 * </ul>
 *
 * <p>解析失败（格式错误、claim 缺失等）返回 {@code null}，调用方不阻断请求。</p>
 */
@UtilityClass
public class BearerJwtPayloadReader {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 从 {@code Authorization: Bearer ...} 值中提取 userId；解析失败返回 null。
     */
    public static String readUserId(String authorizationHeader) {
        return readClaimFromAuthorization(authorizationHeader, InJwtClaimNames.ID);
    }

    /**
     * 从 {@code Authorization: Bearer ...} 值中提取会话 ID（claim {@code sid}）；解析失败返回 null。
     */
    public static String readSid(String authorizationHeader) {
        return readClaimFromAuthorization(authorizationHeader, InJwtClaimNames.SID);
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (StrUtil.isBlank(authorizationHeader)) {
            return null;
        }
        if (!StrUtil.startWithIgnoreCase(authorizationHeader, BEARER_PREFIX)) {
            return null;
        }
        return StrUtil.emptyToNull(authorizationHeader.substring(BEARER_PREFIX.length()).trim());
    }

    private static String readClaimFromAuthorization(String authorizationHeader, String claimName) {
        return JwtPayloadUtil.readClaim(extractBearerToken(authorizationHeader), claimName);
    }
}
