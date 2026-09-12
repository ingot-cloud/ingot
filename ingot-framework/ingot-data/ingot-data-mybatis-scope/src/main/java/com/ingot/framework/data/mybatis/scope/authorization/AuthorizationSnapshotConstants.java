package com.ingot.framework.data.mybatis.scope.authorization;

/**
 * <p>授权快照缓存与期限的封闭常量，禁止在调用点复制字面量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AuthorizationSnapshotConstants {

    /**
     * 分层缓存实例名。
     */
    public static final String CACHE_NAME = "authorization-snapshot";

    /**
     * L2 Redis key 前缀，后接 {@code tenantId:userId}。
     */
    public static final String REDIS_KEY_PREFIX = "in:auth:snapshot:";

    /**
     * 授权快照最大存活秒数，{@code expiresAt} 不得超过 {@code generatedAt} 加上该值。
     */
    public static final int MAX_TTL_SECONDS = 30;

    /**
     * 跨节点时钟偏差的提前过期秒数。
     */
    public static final int CLOCK_SKEW_SECONDS = 1;

    /**
     * 内部快照接口路径前缀，过滤器跳过以免递归加载。
     */
    public static final String INNER_PATH_PREFIX = "/inner/";

    /**
     * 快照来源：源端实时组装，对应分层缓存 {@code REMOTE}。
     */
    public static final String SOURCE_REMOTE = "REMOTE";

    private AuthorizationSnapshotConstants() {
    }
}
