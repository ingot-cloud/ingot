package com.ingot.framework.data.mybatis.scope.authorization;

import java.time.Instant;

import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.data.mybatis.scope.error.AuthorizationSnapshotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>授权快照读取入口：命中后校验绝对期限，过期则失效再加载；刷新失败抛 503 语义异常。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AuthorizationSnapshotAccess {

    private final LayeredCache<String, AuthorizationSnapshotDTO> cache;

    /**
     * 读取当前租户用户的有效授权快照。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 未过期快照
     * @throws AuthorizationSnapshotException 远端不可用或过期刷新失败
     */
    public AuthorizationSnapshotDTO require(long tenantId, long userId) {
        String key = cacheKey(tenantId, userId);
        try {
            AuthorizationSnapshotDTO snapshot = cache.get(key);
            if (isExpired(snapshot)) {
                cache.evict(key);
                snapshot = cache.get(key);
            }
            if (snapshot == null || isExpired(snapshot)) {
                throw new AuthorizationSnapshotException();
            }
            return snapshot;
        } catch (RemoteUnavailableException ex) {
            throw new AuthorizationSnapshotException(ex);
        }
    }

    /**
     * 清除全部热缓存，不影响其它业务缓存。
     */
    public void evictAll() {
        cache.evictAll();
    }

    /**
     * 拼接租户与用户缓存键。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return {@code tenantId:userId}
     */
    public static String cacheKey(long tenantId, long userId) {
        return tenantId + ":" + userId;
    }

    private static boolean isExpired(AuthorizationSnapshotDTO snapshot) {
        if (snapshot == null || snapshot.getExpiresAt() == null) {
            return true;
        }
        return Instant.now().isAfter(snapshot.getExpiresAt());
    }
}
