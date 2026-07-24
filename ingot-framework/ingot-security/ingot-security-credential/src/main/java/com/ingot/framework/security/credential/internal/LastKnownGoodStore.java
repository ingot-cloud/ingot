package com.ingot.framework.security.credential.internal;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 最近成功快照（Last-Known-Good, LKG）存储。
 *
 * <p>与 L1/L2 热缓存在物理与生命周期上完全分离：使用独立 Redis key、长存 / 不过期，仅当远程数据源
 * 成功返回（含合法空）时由 {@link ResilientCredentialPolicyConfigService} 刷新；失效事件不清 LKG。</p>
 *
 * <p>Redis 是 LKG 的<b>唯一</b>存储，不再持进程内副本：多节点从同一 Redis key 读取，保证降级来源
 * 跨节点一致；当 Redis 不可用（或 key 缺失）时 {@link #load()} 返回 {@code null}，由上层统一落 Nacos 地板，
 * 避免"某些节点用进程内 LKG、某些节点用地板"的跨节点分叉。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class LastKnownGoodStore {

    private static final TypeReference<List<CredentialPolicyConfigVO>> LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String key;
    private final Duration ttl;

    /**
     * @param redisTemplate Redis 模板；为 {@code null} 时 LKG 不可用（save 空操作、load 返回 null，交由地板兜底）
     * @param objectMapper  JSON 序列化器
     * @param key           LKG 独立 key（与热缓存前缀区分命名空间）
     * @param ttl           过期时间；{@code null} 或非正表示不过期（长存）
     */
    public LastKnownGoodStore(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              String key,
                              Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.key = key;
        this.ttl = ttl;
    }

    /**
     * 刷新 LKG（仅远程成功时调用，允许覆盖为空以表达「合法无策略」）。
     * <p>Redis 不可用时为空操作——LKG 仅依赖 Redis 共享，不再写进程内副本。</p>
     */
    public void save(List<CredentialPolicyConfigVO> data) {
        if (redisTemplate == null || objectMapper == null) {
            return;
        }
        List<CredentialPolicyConfigVO> snapshot = data != null ? data : List.of();
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, json, ttl);
            } else {
                redisTemplate.opsForValue().set(key, json);
            }
        } catch (Exception e) {
            log.warn("[Credential] LKG save failed key={}", key, e);
        }
    }

    /**
     * 读取 LKG 快照；不存在 / Redis 不可用返回 {@code null}（区别于「成功空」的空集合）。
     * <p>仅读 Redis（跨节点共享的唯一 LKG 源）；读失败或 key 缺失时返回 {@code null}，
     * 由 {@link ResilientCredentialPolicyConfigService} 落 Nacos 地板，保证多节点降级来源一致。</p>
     */
    public List<CredentialPolicyConfigVO> load() {
        if (redisTemplate == null || objectMapper == null) {
            return null;
        }
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw != null && !raw.isEmpty()) {
                return objectMapper.readValue(raw, LIST_TYPE);
            }
        } catch (Exception e) {
            log.warn("[Credential] LKG load failed key={}, fall through to Nacos floor", key, e);
        }
        return null;
    }
}
