package com.ingot.cloud.security.api.event;

/**
 * 安全策略域枚举。
 *
 * <p>每个域对应 ingot-service-security 内一类可页面化管理的配置：</p>
 * <ul>
 *     <li>{@link #RATE_LIMIT_RULE} — 网关限流规则</li>
 *     <li>{@link #ENDPOINT_GROUP} — API 路径分组定义</li>
 *     <li>{@link #IP_LIST} — IP / 设备 / 用户 / CIDR / UA 黑白名单</li>
 *     <li>{@link #CHALLENGE_POLICY} — 挑战策略（限流命中后要求验证码）</li>
 *     <li>{@link #ALL} — 全量失效，订阅方清空所有域缓存</li>
 * </ul>
 *
 * @author jy
 * @since 2026/5/26
 */
public enum SecurityPolicyDomain {

    ALL,
    RATE_LIMIT_RULE,
    ENDPOINT_GROUP,
    IP_LIST,
    CHALLENGE_POLICY
}
