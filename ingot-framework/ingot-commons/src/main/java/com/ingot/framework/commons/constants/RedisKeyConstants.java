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

    /** Key 各段之间的分隔符。 */
    String SEPARATOR = ":";

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

        /** 登录失败策略 L2 热缓存：{@code in:sec:lf:policy:snapshot}。 */
        String POLICY_SNAPSHOT = PREFIX + ":policy:snapshot";
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

    /**
     * 会话策略分层缓存 Redis Key（Auth 侧并发策略 L2 / LKG）。
     */
    interface SessionPolicy {

        String PREFIX = IN_PREFIX + ":sec:session";

        /** 并发策略 L2 热缓存：{@code in:sec:session:concurrency}。 */
        String CONCURRENCY_SNAPSHOT = PREFIX + ":concurrency";

        /** 并发策略 LKG 快照：{@code in:sec:session:concurrency:lkg}。 */
        String CONCURRENCY_LKG = PREFIX + ":concurrency:lkg";
    }

    /**
     * 账号锁定策略分层缓存 Redis Key（PMS / Member 侧 lockout 策略 L2 / LKG）。
     * <p>与 {@link AccountLock} 锁定信号命名空间独立。</p>
     */
    interface AccountLockoutPolicy {

        String PREFIX = IN_PREFIX + ":sec:account:policy";

        /** L2 热缓存：{@code in:sec:account:policy:snapshot}。 */
        String SNAPSHOT = PREFIX + ":snapshot";

        /** LKG 快照：{@code in:sec:account:policy:lkg}。 */
        String LKG = PREFIX + ":lkg";
    }

    /**
     * 账号锁定信号 Redis Key（BFF / Gateway / Auth 分层拦截；与网关 temp-block 命名空间独立）。
     */
    interface AccountLock {

        String PREFIX = IN_PREFIX + ":sec:account:locked";

        /** 按用户 ID：{@code in:sec:account:locked:uid:{userType}:{userId}}。 */
        String UID_PREFIX = PREFIX + ":uid:";

        /** 按用户名：{@code in:sec:account:locked:name:{userType}:{username}}。 */
        String NAME_PREFIX = PREFIX + ":name:";

        static String uidKey(String userType, Long userId) {
            return UID_PREFIX + userType + ":" + userId;
        }

        static String nameKey(String userType, String username) {
            return NAME_PREFIX + userType + ":" + username;
        }
    }

    /**
     * 在线会话 Redis Key（Auth 签发瘦身 JWT 后的扩展信息；Gateway 按 sid 读取 userType）。
     *
     * <p>会话主键为 sid（等于 {@code OAuth2Authorization.id}），refresh 换发不变。
     * {@link #JTI_PREFIX} 与 {@link #USER_UNIQUE_PREFIX} 运行时不再写入，仅供发布清存量脚本匹配。</p>
     *
     * <p>前缀延续现网命名（无 {@link #IN_PREFIX}），与 {@code RedisOnlineTokenService} 历史
     * key 空间对齐，便于运维脚本按同一 pattern 扫描。</p>
     */
    interface OnlineToken {

        /** 会话主数据：{@code token:sid:{sid}}。 */
        String SID_PREFIX = "token:sid:";

        /**
         * 历史 Access Token 索引前缀：{@code token:jti:{jti}}。
         * <p>运行时已不再写入；发布脚本仍按此 pattern 清存量。</p>
         */
        String JTI_PREFIX = "token:jti:";

        /**
         * 历史单会话索引前缀：{@code token:user:{tenantId}:{clientId}:{userId}}。
         * <p>运行时已不再写入；与 {@link #USER_SET_PREFIX} 不同，后者仍在使用。
         * 发布脚本 {@code token:user:*} 会同时覆盖本前缀与用户会话集合。</p>
         */
        String USER_UNIQUE_PREFIX = "token:user:";

        /** 用户会话集合：{@code token:user:set:{tenantId}:{clientId}:{userId}} → Set&lt;sid&gt;。 */
        String USER_SET_PREFIX = "token:user:set:";

        /** 在线用户排序集：{@code online:user:{tenantId}:{clientId}} → ZSet&lt;userId, expiresAtMs&gt;。 */
        String ONLINE_USER_PREFIX = "online:user:";

        /** 同 IP 会话集合：{@code session:ip:{tenantId}:{ip}} → Set&lt;sid&gt;。 */
        String IP_SET_PREFIX = "session:ip:";

        /**
         * 在线用户 ZSet 注册表：{@code session:online:registry}，成员为
         * {@link #ONLINE_USER_PREFIX} 系列 key。
         *
         * <p>定时清理据此枚举待清理的 ZSet，避免对生产 Redis 执行 {@code KEYS}。</p>
         */
        String ONLINE_REGISTRY = "session:online:registry";

        static String sidKey(String sid) {
            return SID_PREFIX + sid;
        }

        static String userSetKey(Long tenantId, String clientId, Long userId) {
            return USER_SET_PREFIX + userScope(tenantId, clientId, userId);
        }

        static String onlineUserKey(Long tenantId, String clientId) {
            return ONLINE_USER_PREFIX + tenantId + SEPARATOR + clientId;
        }

        /**
         * 在线用户排序集的租户前缀：{@code online:user:{tenantId}:}。
         *
         * <p>跨 Client 查询据此从 {@link #ONLINE_REGISTRY} 成员中筛出本租户的 ZSet，
         * 无需扫描 key 空间。</p>
         */
        static String onlineUserTenantPrefix(Long tenantId) {
            return ONLINE_USER_PREFIX + tenantId + SEPARATOR;
        }

        /**
         * 从在线用户排序集 key 中解析 clientId。
         *
         * @return clientId；key 不属于该租户时返回 {@code null}
         */
        static String parseClientId(String onlineUserKey, Long tenantId) {
            String prefix = onlineUserTenantPrefix(tenantId);
            if (onlineUserKey == null || !onlineUserKey.startsWith(prefix)) {
                return null;
            }
            return onlineUserKey.substring(prefix.length());
        }

        /**
         * 从在线用户排序集 key 中解析租户 ID。
         *
         * @return 租户 ID；key 不符合 {@code online:user:{tenantId}:{clientId}} 时返回 {@code null}
         */
        static Long parseTenantId(String onlineUserKey) {
            if (onlineUserKey == null || !onlineUserKey.startsWith(ONLINE_USER_PREFIX)) {
                return null;
            }
            String rest = onlineUserKey.substring(ONLINE_USER_PREFIX.length());
            int sep = rest.indexOf(SEPARATOR);
            if (sep <= 0) {
                return null;
            }
            try {
                return Long.parseLong(rest.substring(0, sep));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        static String ipSetKey(Long tenantId, String ip) {
            return IP_SET_PREFIX + tenantId + SEPARATOR + ip;
        }

        private static String userScope(Long tenantId, String clientId, Long userId) {
            return tenantId + SEPARATOR + clientId + SEPARATOR + userId;
        }
    }
}
