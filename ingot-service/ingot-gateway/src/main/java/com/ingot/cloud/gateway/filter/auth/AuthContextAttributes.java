package com.ingot.cloud.gateway.filter.auth;

import lombok.experimental.UtilityClass;

/**
 * {@link AuthContextRelayFilter} 写入的 exchange attribute 键定义。
 *
 * <p>与 {@link com.ingot.cloud.gateway.security.GatewaySecurityConstants} 分工：
 * 本类收纳 auth 子包中间态；最终身份由 {@link IdentityResolveFilter} 聚合为
 * {@link com.ingot.cloud.gateway.security.ClientIdentity} 并以
 * {@link com.ingot.cloud.gateway.security.GatewaySecurityConstants#ATTR_CLIENT_IDENTITY} 对外暴露。</p>
 */
@UtilityClass
public class AuthContextAttributes {

    /**
     * JWT 解析出的用户 ID。
     * <p>由 {@link AuthContextRelayFilter} 写入；{@link IdentityResolveFilter} 读取后
     * 填入 {@link com.ingot.cloud.gateway.security.ClientIdentity#getUserId()}，
     * 非空时同步回填 {@code X-User-Id} Header 供 Sentinel {@code USER} 维度。</p>
     */
    public static final String USER_ID = "ingot.security.auth.userId";
}
