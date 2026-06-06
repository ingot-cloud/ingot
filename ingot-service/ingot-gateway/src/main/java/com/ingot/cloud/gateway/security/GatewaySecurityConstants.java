package com.ingot.cloud.gateway.security;

import lombok.experimental.UtilityClass;

/**
 * 网关安全策略执行面常量：Redis Key 前缀、Exchange Attribute 键、业务错误码、响应文案、违规升级阈值等。
 *
 * <p>与 {@link com.ingot.framework.commons.constants.HeaderConstants}（HTTP Header）、
 * {@link com.ingot.framework.vc.common.VCConstants}（验证码 PassToken 参数）分工明确，
 * 本类为网关 security 包内运行时常量的唯一事实来源。</p>
 *
 * <h3>相关配置示例</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     ratelimit:
 *       enabled: true              # 为 true 时 Sentinel 编译 SDK 规则
 *     blacklist:
 *       enabled: true              # 静态名单 + 读取 Redis 临时封禁
 *     challenge:
 *       enabled: true              # ALWAYS / ON_RATE_LIMIT 挑战
 * spring:
 *   cloud:
 *     sentinel:
 *       scg:
 *         enabled: true            # 默认 true，须开启才有 SentinelGatewayFilter
 * }</pre>
 *
 * @author jy
 * @since 2026/6/4
 */
@UtilityClass
public class GatewaySecurityConstants {

    // ---- Redis Key 前缀 ----

    /** 临时封禁 Key 前缀：{@code in:gw:bl:tmp:{keyType}:{keyValue}}。 */
    public static final String REDIS_KEY_TEMP_BLOCK_PREFIX = "in:gw:bl:tmp:";

    /** 限流违规计数 Key 前缀：{@code in:gw:violation:{keyType}:{keyValue}:{ruleCode}}。 */
    public static final String REDIS_KEY_VIOLATION_PREFIX = "in:gw:violation:";

    /** PassToken Key 前缀：{@code in:gw:vc:pass:{scope}:{token}}。 */
    public static final String REDIS_KEY_PASS_TOKEN_PREFIX = "in:gw:vc:pass:";

    // ---- Exchange Attribute 键 ----

    /** {@link ClientIdentity} 在 exchange 中的 attribute 键。 */
    public static final String ATTR_CLIENT_IDENTITY = "ingot.security.identity";

    /** 静态白名单命中标记，供挑战与 Sentinel 跳过。 */
    public static final String ATTR_WHITELISTED = "ingot.security.whitelisted";

    /** PassToken 验码成功标记，供 Sentinel 跳过限流。 */
    public static final String ATTR_PASS_TOKEN_OK = "ingot.security.passToken.ok";

    // ---- 业务错误码（HTTP 响应 body.code） ----

    /** 静态/临时黑名单拒绝。HTTP 403。 */
    public static final String CODE_FORBIDDEN_BLOCKED = "FORBIDDEN_BLOCKED";

    /** Sentinel 限流拒绝（无 ON_RATE_LIMIT 挑战策略时）。HTTP 429。 */
    public static final String CODE_LIMIT_TOO_MANY = "LIMIT_TOO_MANY";

    /** 需要验证码挑战。HTTP 412。 */
    public static final String CODE_CHALLENGE_REQUIRED = "CHALLENGE_REQUIRED";

    // ---- 响应文案 ----

    public static final String MSG_REQUEST_BLOCKED = "Request blocked";
    public static final String MSG_TOO_MANY_REQUESTS = "Too many requests";
    public static final String MSG_CAPTCHA_REQUIRED = "Captcha required";

    // ---- 审计 / 违规规则编码 ----

    /** 限流触发自动封禁时写入审计的 ruleCode。 */
    public static final String RULE_CODE_RATE_LIMIT = "RATE_LIMIT";

    // ---- PassToken ----

    /** 策略未配置 scope 时的默认 PassToken 作用域。 */
    public static final String DEFAULT_PASS_TOKEN_SCOPE = "default";

    // ---- 限流违规升级（Phase 1 阈值未配置化，见 SentinelBlockHandler） ----

    /** 滑动窗口内限流拒绝次数达到该值即临时封禁。 */
    public static final long VIOLATION_BLOCK_THRESHOLD = 30L;

    /** 违规计数滑动窗口（秒）。 */
    public static final long VIOLATION_WINDOW_SECONDS = 60L;

    /** 临时封禁 TTL（分钟）。 */
    public static final long TEMP_BLOCK_TTL_MINUTES = 15L;

    /** 429 响应 {@code Retry-After} 头（秒）。 */
    public static final String RETRY_AFTER_SECONDS = "1";

    /** 违规计数 Redis TTL 下限（毫秒），避免过短窗口导致 key 无意义。 */
    public static final long MIN_VIOLATION_WINDOW_MS = 1000L;

    /** Sentinel 编译时 intervalSec 下限。 */
    public static final int MIN_RATE_LIMIT_INTERVAL_SEC = 1;

    /** PassToken 签发时 remaining / ttl 下限。 */
    public static final int MIN_PASS_TOKEN_REMAINING = 1;
}
