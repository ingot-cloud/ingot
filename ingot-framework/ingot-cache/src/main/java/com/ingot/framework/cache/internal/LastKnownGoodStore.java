package com.ingot.framework.cache.internal;

import java.time.Duration;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>最近成功快照（Last-Known-Good）存储，远端不可用时的第一级兜底数据源。</p>
 *
 * <p>与 L1/L2 热缓存在物理与生命周期上完全分离：使用独立 Redis key、默认长存不过期，
 * 仅在远端<b>成功</b>返回（含合法空）时刷新，失效广播与 {@code evictAll} 都不会清除它。
 * 这是「策略变更后清缓存，但故障时仍有最后一份可用数据」得以成立的前提。</p>
 *
 * <p>Redis 是唯一存储，不持进程内副本：多节点从同一 key 读取，保证降级来源跨节点一致；
 * Redis 不可用或 key 缺失时 {@link #load} 返回 {@code null}，由上层统一落地板，
 * 避免出现「部分节点用 LKG、部分节点用地板」的分叉。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see ResilientCacheLayer
 */
@Slf4j
public class LastKnownGoodStore<K, V> {

    private final String name;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TypeReference<V> valueType;
    private final Function<K, String> keyResolver;
    private final Duration ttl;

    /**
     * @param name          缓存实例名，仅用于日志
     * @param redisTemplate Redis 模板；{@code null} 时 LKG 整体降级为不可用
     * @param objectMapper  JSON 序列化器；{@code null} 时同上
     * @param valueType     值类型引用，用于反序列化泛型
     * @param keyResolver   缓存键到 Redis key 的映射；单 key 场景可返回固定串
     * @param ttl           过期时间；{@code null} 或非正表示长存不过期
     */
    public LastKnownGoodStore(String name,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              TypeReference<V> valueType,
                              Function<K, String> keyResolver,
                              Duration ttl) {
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.valueType = valueType;
        this.keyResolver = keyResolver;
        this.ttl = ttl;
    }

    /**
     * 判断 LKG 是否具备可用的底层存储。
     *
     * @return Redis 与序列化器均就绪时为 {@code true}
     */
    public boolean available() {
        return redisTemplate != null && objectMapper != null;
    }

    /**
     * 刷新最近成功快照，仅应在远端成功返回时调用。
     *
     * @param key   缓存键
     * @param value 远端数据；{@code null} 时跳过写入，避免把「加载失败」固化为 LKG
     */
    public void save(K key, V value) {
        if (!available() || value == null) {
            return;
        }
        String redisKey = keyResolver.apply(key);
        try {
            String json = objectMapper.writeValueAsString(value);
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(redisKey, json, ttl);
            } else {
                redisTemplate.opsForValue().set(redisKey, json);
            }
        } catch (Exception e) {
            log.warn("[Cache:{}] LKG save failed key={}", name, redisKey, e);
        }
    }

    /**
     * 读取最近成功快照。
     *
     * @param key 缓存键
     * @return 快照；key 缺失、Redis 不可用或反序列化失败时为 {@code null}
     */
    public V load(K key) {
        if (!available()) {
            return null;
        }
        String redisKey = keyResolver.apply(key);
        try {
            String raw = redisTemplate.opsForValue().get(redisKey);
            if (raw != null && !raw.isEmpty()) {
                return objectMapper.readValue(raw, valueType);
            }
        } catch (Exception e) {
            log.warn("[Cache:{}] LKG load failed key={}, fall through to floor", name, redisKey, e);
        }
        return null;
    }
}
