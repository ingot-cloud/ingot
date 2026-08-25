package com.ingot.framework.dict.client.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;
import lombok.RequiredArgsConstructor;

/**
 * <p>字典客户端的缓存入口，把 {@link DictService} 接到分层缓存链上，验证泛型 {@code K} 不被单 key 场景带偏。</p>
 *
 * <p>键为 {@link DictCacheKey}（code + scope + 租户/应用 + 是否含禁用项）。
 * {@link #evict(String)} 按 code 清掉该编码下所有 query 变体；{@link #evictAll()} 清全部。
 * 不启用 LKG 与地板，远端失败语义仍由 delegate 自己决定（当前为返回空）。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class LayeredDictService implements DictService {

    private final LayeredCache<DictCacheKey, List<DictItem>> cache;
    private final String keyPrefix;

    @Override
    public List<DictItem> items(String dictCode, DictQuery query) {
        List<DictItem> data = cache.get(DictCacheKey.of(dictCode, query));
        return data != null ? data : List.of();
    }

    @Override
    public Map<String, List<DictItem>> batchItems(List<String> dictCodes, DictQuery query) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, List<DictItem>> ordered = new LinkedHashMap<>(dictCodes.size());
        for (String code : dictCodes) {
            ordered.put(code, items(code, query));
        }
        return ordered;
    }

    @Override
    public void evict(String dictCode) {
        if (dictCode == null || dictCode.isBlank()) {
            return;
        }
        cache.evictMatching(key -> dictCode.equals(key.code()),
                DictCacheKey.redisPattern(keyPrefix, dictCode));
    }

    @Override
    public void evictAll() {
        cache.evictAll();
    }
}
