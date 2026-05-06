package com.ingot.framework.dict.client.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.config.DictClientProperties;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;
import com.ingot.framework.dict.client.model.DictScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * L2 Redis 共享缓存装饰器。所有 dict-client 实例可通过共享 Redis 命中同一份字典缓存，
 * 避免高频 RPC；命中失败时回源 delegate 并写回。
 * <p>
 * Key 格式：{@code <prefix>:dict:items:<dictCode>:<scope>:<tenantId>:<appId>:<includeDisabled>}。
 * 默认前缀 {@code in}（与 {@code CacheConstants.IGNORE_TENANT_PREFIX} 一致），平台级共享、不绑租户。
 * <p>
 * <b>不缓存空列表</b>：若把 {@code []} 写入 Redis，后续字典从「无项」变为「有项」时，
 * 一旦跨节点失效未及时到达，调用方会长期命中 L2 的空数组而不再回源（典型现象：
 * PMS 已能查到新项，其它服务仍返回 0 条）。空结果不写键；读到的空数组视为未命中并删键。
 * </p>
 *
 * @author jy
 * @since 2026/4/27
 */
@Slf4j
public class RedisDictService implements DictService {

    private static final String KEY_NAMESPACE = "dict:items";
    private static final TypeReference<List<DictItem>> LIST_TYPE = new TypeReference<>() {
    };

    private final DictService delegate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Duration ttl;

    public RedisDictService(DictService delegate,
                            StringRedisTemplate redisTemplate,
                            ObjectMapper objectMapper,
                            DictClientProperties properties) {
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = properties.getRedisKeyPrefix();
        this.ttl = properties.getRedisTtl();
    }

    @Override
    public List<DictItem> items(String dictCode, DictQuery query) {
        String key = buildKey(dictCode, query);
        List<DictItem> hit = readKey(key);
        if (hit != null) {
            return hit;
        }
        List<DictItem> fresh = delegate.items(dictCode, query);
        writeKey(key, fresh);
        return fresh;
    }

    @Override
    public Map<String, List<DictItem>> batchItems(List<String> dictCodes, DictQuery query) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return Map.of();
        }

        List<String> keys = new ArrayList<>(dictCodes.size());
        for (String code : dictCodes) {
            keys.add(buildKey(code, query));
        }

        List<String> raws;
        try {
            raws = redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            log.warn("[Dict] L2 mget failed, falling back to delegate", e);
            return delegate.batchItems(dictCodes, query);
        }

        Map<String, List<DictItem>> result = new LinkedHashMap<>();
        List<String> missingCodes = new ArrayList<>();
        for (int i = 0; i < dictCodes.size(); i++) {
            String code = dictCodes.get(i);
            String raw = raws == null ? null : raws.get(i);
            List<DictItem> parsed = parse(raw);
            if (parsed != null && !parsed.isEmpty()) {
                result.put(code, parsed);
            } else {
                if (parsed != null) {
                    try {
                        redisTemplate.delete(keys.get(i));
                    } catch (Exception e) {
                        log.warn("[Dict] L2 delete stale empty key failed key={}", keys.get(i), e);
                    }
                }
                missingCodes.add(code);
            }
        }

        if (!missingCodes.isEmpty()) {
            Map<String, List<DictItem>> fetched = delegate.batchItems(missingCodes, query);
            fetched.forEach((code, list) -> {
                writeKey(buildKey(code, query), list);
                result.put(code, list);
            });
        }

        // 保持入参顺序
        Map<String, List<DictItem>> ordered = new LinkedHashMap<>(dictCodes.size());
        for (String code : dictCodes) {
            ordered.put(code, result.getOrDefault(code, List.of()));
        }
        return ordered;
    }

    @Override
    public void evict(String dictCode) {
        if (dictCode != null && !dictCode.isBlank()) {
            String pattern = keyPrefix + ":" + KEY_NAMESPACE + ":" + dictCode + ":*";
            deleteByPattern(pattern);
        }
        delegate.evict(dictCode);
    }

    @Override
    public void evictAll() {
        String pattern = keyPrefix + ":" + KEY_NAMESPACE + ":*";
        deleteByPattern(pattern);
        delegate.evictAll();
    }

    private List<DictItem> readKey(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            List<DictItem> list = parse(raw);
            if (list != null && list.isEmpty()) {
                try {
                    redisTemplate.delete(key);
                } catch (Exception e) {
                    log.warn("[Dict] L2 delete stale empty key failed key={}", key, e);
                }
                return null;
            }
            return list;
        } catch (Exception e) {
            log.warn("[Dict] L2 read failed key={}", key, e);
            return null;
        }
    }

    private void writeKey(String key, List<DictItem> value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("[Dict] L2 write failed key={}", key, e);
        }
    }

    private List<DictItem> parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, LIST_TYPE);
        } catch (Exception e) {
            log.warn("[Dict] L2 deserialize failed, raw={}", raw, e);
            return null;
        }
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = scanKeys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                if (log.isDebugEnabled()) {
                    log.debug("[Dict] L2 evict pattern={}, count={}", pattern, keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("[Dict] L2 evict failed pattern={}", pattern, e);
        }
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }
        return result;
    }

    private String buildKey(String dictCode, DictQuery query) {
        DictScope scope = query == null || query.getScope() == null ? DictScope.PLATFORM : query.getScope();
        Long tenantId = query == null ? null : query.getTenantId();
        Long appId = query == null ? null : query.getAppId();
        boolean includeDisabled = query != null && query.isIncludeDisabled();
        return keyPrefix + ":" + KEY_NAMESPACE + ":" + dictCode
                + ":" + scope.name()
                + ":" + (tenantId == null ? "_" : tenantId)
                + ":" + (appId == null ? "_" : appId)
                + ":" + (includeDisabled ? "1" : "0");
    }
}
