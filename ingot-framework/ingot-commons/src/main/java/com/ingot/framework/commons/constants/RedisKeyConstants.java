package com.ingot.framework.commons.constants;

/**
 * <p>跨模块共用的 Redis Key 前缀与拼装规则，避免网关、Auth、策略客户端各自硬编码相同命名空间。</p>
 *
 * <p>实体 CRUD 的 {@code @Cacheable} 缓存命名见 {@link CacheConstants}；本类仅覆盖安全策略执行面、
 * 登录失败防护等运行时 Redis 结构。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface RedisKeyConstants {

    /** 平台级 Redis 命名空间根前缀，与 {@link CacheConstants#IGNORE_TENANT_PREFIX} 一致。 */
    String IN_PREFIX = CacheConstants.IGNORE_TENANT_PREFIX;

    /**
     * 网关安全执行面 Redis Key（临时封禁、违规计数、PassToken 等）。
     */
    interface Gateway {

        String PREFIX = IN_PREFIX + ":gw";

        /** 临时封禁：{@code in:gw:bl:tmp:{keyType}:{keyValue}}。 */
        String TEMP_BLOCK_PREFIX = PREFIX + ":bl:tmp:";

        /** 限流违规计数：{@code in:gw:violation:{keyType}:{keyValue}:{ruleCode}}。 */
        String VIOLATION_PREFIX = PREFIX + ":violation:";

        /** PassToken：{@code in:gw:vc:pass:{scope}:{token}}。 */
        String PASS_TOKEN_PREFIX = PREFIX + ":vc:pass:";

        static String tempBlockKey(String keyType, String keyValue) {
            return TEMP_BLOCK_PREFIX + keyType + ":" + keyValue;
        }

        static String violationKey(String keyType, String keyValue, String ruleCode) {
            return VIOLATION_PREFIX + keyType + ":" + keyValue + ":" + ruleCode;
        }

        static String passTokenKey(String scope, String token) {
            return PASS_TOKEN_PREFIX + scope + ":" + token;
        }
    }

    /**
     * 登录失败防护 Redis Key（Auth 侧计数；临时封禁与 {@link Gateway#TEMP_BLOCK_PREFIX} 共用）。
     */
    interface LoginFailure {

        String PREFIX = IN_PREFIX + ":sec:lf";

        /** 失败计数：{@code in:sec:lf:{dimension}:{...}}。 */
        String COUNTER_PREFIX = PREFIX + ":";

        /** 登录失败策略 LKG 快照：{@code in:sec:lf:policy:lkg}。 */
        String POLICY_LKG = PREFIX + ":policy:lkg";
    }

    /**
     * 安全策略快照分层缓存 Redis Key（gateway-rule-client 共享快照 L2 / LKG）。
     */
    interface SecurityPolicy {

        String PREFIX = IN_PREFIX + ":sec:policy";

        /** LKG 快照：{@code in:sec:policy:lkg:snapshot}。 */
        String LKG_SNAPSHOT = PREFIX + ":lkg:snapshot";

        /** L2 热缓存：{@code in:sec:policy:snapshot}。 */
        String SNAPSHOT = PREFIX + ":snapshot";
    }
}
