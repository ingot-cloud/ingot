package com.ingot.framework.cache.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.cache.spi.LayeredCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>L2 Redis 共享缓存层，让集群内所有节点命中同一份数据，同时为跨节点一致性提供 TTL 兜底。</p>
 *
 * <p>不缓存「不合格」的值（由 {@code cacheable} 判定，默认为非 null）：写入前拦截，读到时主动删除并当作 miss
 * 穿透。这样可避免规则切换窗口期把空结果固化成长期命中的 stale empty。</p>
 *
 * <p>{@link #evictAll()} 按 {@code evictAllPattern} 决定清理方式：不含通配符时直接 DEL 单键（单 key 场景），
 * 含通配符时用 SCAN 游标批量删除（多 key 场景），不使用会阻塞 Redis 的 KEYS 命令。</p>
 *
 * <p>所有 Redis 异常均降级为日志并当作 miss 处理，不向上传播——L2 是加速层，不应成为故障源。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class RedisCacheLayer<K, V> implements LayeredCache<K, V> {

    private static final int SCAN_BATCH = 256;

    private final String name;
    private final LayeredCache<K, V> delegate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TypeReference<V> valueType;
    private final Function<K, String> keyResolver;
    private final String evictAllPattern;
    private final Duration ttl;
    private final Predicate<V> cacheable;

    public RedisCacheLayer(String name,
                           LayeredCache<K, V> delegate,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           TypeReference<V> valueType,
                           Function<K, String> keyResolver,
                           String evictAllPattern,
                           Duration ttl,
                           Predicate<V> cacheable) {
        this.name = name;
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.valueType = valueType;
        this.keyResolver = keyResolver;
        this.evictAllPattern = evictAllPattern;
        this.ttl = ttl;
        this.cacheable = cacheable;
    }

    @Override
    public V get(K key) {
        V hit = read(key);
        if (hit != null) {
            return hit;
        }
        V fresh = delegate.get(key);
        write(key, fresh);
        return fresh;
    }

    @Override
    public void evict(K key) {
        String redisKey = keyResolver.apply(key);
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("[Cache:{}] L2 evict failed key={}", name, redisKey, e);
        }
        delegate.evict(key);
    }

    @Override
    public void evictAll() {
        try {
            if (hasWildcard(evictAllPattern)) {
                scanAndDelete();
            } else {
                redisTemplate.delete(evictAllPattern);
            }
        } catch (Exception e) {
            log.warn("[Cache:{}] L2 evictAll failed pattern={}", name, evictAllPattern, e);
        }
        delegate.evictAll();
    }

    @Override
    public String name() {
        return name;
    }

    private void scanAndDelete() {
        ScanOptions options = ScanOptions.scanOptions().match(evictAllPattern).count(SCAN_BATCH).build();
        List<String> batch = new ArrayList<>(SCAN_BATCH);
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= SCAN_BATCH) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            redisTemplate.delete(batch);
        }
    }

    private V read(K key) {
        String redisKey = keyResolver.apply(key);
        try {
            String raw = redisTemplate.opsForValue().get(redisKey);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            V value = objectMapper.readValue(raw, valueType);
            if (!cacheable.test(value)) {
                deleteStale(redisKey);
                return null;
            }
            return value;
        } catch (Exception e) {
            log.warn("[Cache:{}] L2 read failed key={}", name, redisKey, e);
            return null;
        }
    }

    private void write(K key, V value) {
        if (!cacheable.test(value)) {
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
            log.warn("[Cache:{}] L2 write failed key={}", name, redisKey, e);
        }
    }

    private void deleteStale(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("[Cache:{}] L2 delete stale key failed key={}", name, redisKey, e);
        }
    }

    private static boolean hasWildcard(String pattern) {
        return pattern != null
                && (pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0 || pattern.indexOf('[') >= 0);
    }
}
