package com.ingot.framework.security.access.service;

import java.time.Duration;

/**
 * 临时封禁写入端口。
 *
 * @author jy
 * @since 1.0.0
 */
public interface TempBlockWriter {

    /**
     * 覆盖写入临时封禁（兼容旧调用方）。
     *
     * @param keyType  键类型
     * @param keyValue 键值
     * @param ttl      TTL
     */
    void block(String keyType, String keyValue, Duration ttl);

    /**
     * 首次占位写入临时封禁（SET NX）。
     *
     * @param keyType  键类型
     * @param keyValue 键值
     * @param ttl      TTL
     * @return {@code true} 表示首次写入成功；已存在或失败返回 {@code false}
     */
    default boolean tryBlockFirst(String keyType, String keyValue, Duration ttl) {
        if (isBlocked(keyType, keyValue)) {
            refreshTtl(keyType, keyValue, ttl);
            return false;
        }
        block(keyType, keyValue, ttl);
        return true;
    }

    /**
     * 判断临时封禁是否已存在。
     *
     * @param keyType  键类型
     * @param keyValue 键值
     * @return 是否已封禁
     */
    default boolean isBlocked(String keyType, String keyValue) {
        return false;
    }

    /**
     * 刷新已有临时封禁的 TTL，不视为边沿。
     *
     * @param keyType  键类型
     * @param keyValue 键值
     * @param ttl      新 TTL
     */
    default void refreshTtl(String keyType, String keyValue, Duration ttl) {
        // no-op
    }
}
