package com.ingot.framework.dict.client.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;
import com.ingot.framework.dict.client.model.DictScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>字典多 key 分层缓存：按 code 失效粒度、空值不缓存、四种查询变体互不覆盖。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class LayeredDictServiceTest {

    private static DictItem item(String code, String value) {
        return DictItem.builder().code(code).value(value).label(value).item(true).build();
    }

    private LayeredDictService service(CountingDelegate delegate) {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(true)
                .l2Enabled(false)
                .resilienceEnabled(false)
                .build();
        LayeredCache<DictCacheKey, List<DictItem>> cache =
                LayeredCacheBuilder.<DictCacheKey, List<DictItem>>named("dict")
                        .loader(key -> delegate.items(key.code(), key.toQuery()))
                        .settings(settings)
                        .cacheable(v -> v != null && !v.isEmpty())
                        .emptyValue(List::of)
                        .build();
        return new LayeredDictService(cache, "in");
    }

    @Test
    @DisplayName("同一 code 重复读取命中 L1")
    void repeatedReadsHitL1() {
        CountingDelegate delegate = new CountingDelegate();
        DictService dict = service(delegate);

        dict.items("user_status", DictQuery.platform());
        dict.items("user_status", DictQuery.platform());

        assertThat(delegate.calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("不同 code 与不同 query 各自独立缓存")
    void keysAreIndependent() {
        CountingDelegate delegate = new CountingDelegate();
        DictService dict = service(delegate);

        dict.items("user_status", DictQuery.platform());
        dict.items("user_status", DictQuery.tenant(1L));
        dict.items("other", DictQuery.platform());
        dict.items("user_status", DictQuery.platform());

        assertThat(delegate.calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("evict(code) 只清该编码的全部 query 变体")
    void evictByCodeClearsAllVariantsOfThatCode() {
        CountingDelegate delegate = new CountingDelegate();
        DictService dict = service(delegate);

        dict.items("user_status", DictQuery.platform());
        dict.items("user_status", DictQuery.tenant(1L));
        dict.items("other", DictQuery.platform());

        dict.evict("user_status");

        dict.items("user_status", DictQuery.platform());
        dict.items("other", DictQuery.platform());

        assertThat(delegate.calls.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("evictAll 清全部编码")
    void evictAllClearsEveryCode() {
        CountingDelegate delegate = new CountingDelegate();
        DictService dict = service(delegate);

        dict.items("user_status", DictQuery.platform());
        dict.items("other", DictQuery.platform());
        dict.evictAll();
        dict.items("user_status", DictQuery.platform());
        dict.items("other", DictQuery.platform());

        assertThat(delegate.calls.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("空列表不写入 L1，下次仍穿透")
    void emptyValuesAreNotCached() {
        CountingDelegate delegate = new CountingDelegate();
        delegate.emptyCodes.add("missing");
        DictService dict = service(delegate);

        assertThat(dict.items("missing", DictQuery.platform())).isEmpty();
        assertThat(dict.items("missing", DictQuery.platform())).isEmpty();
        assertThat(delegate.calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("batchItems 保持入参顺序并复用单 key 缓存")
    void batchItemsPreservesOrderAndL1() {
        CountingDelegate delegate = new CountingDelegate();
        DictService dict = service(delegate);

        dict.items("a", DictQuery.platform());
        Map<String, List<DictItem>> batch = dict.batchItems(List.of("a", "b"), DictQuery.platform());

        assertThat(batch.keySet()).containsExactly("a", "b");
        assertThat(delegate.calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Redis key 格式与迁移前一致")
    void redisKeyFormatUnchanged() {
        DictCacheKey key = DictCacheKey.of("user_status", DictQuery.platform());
        assertThat(key.redisKey("in")).isEqualTo("in:dict:items:user_status:PLATFORM:_:_:0");
        assertThat(DictCacheKey.redisPattern("in", "user_status"))
                .isEqualTo("in:dict:items:user_status:*");
        assertThat(DictCacheKey.redisAllPattern("in")).isEqualTo("in:dict:items:*");

        DictCacheKey tenant = DictCacheKey.of("user_status",
                DictQuery.builder().scope(DictScope.TENANT).tenantId(9L).includeDisabled(true).build());
        assertThat(tenant.redisKey("in")).isEqualTo("in:dict:items:user_status:TENANT:9:_:1");
    }

    private static final class CountingDelegate implements DictService {
        private final AtomicInteger calls = new AtomicInteger();
        private final java.util.Set<String> emptyCodes = new java.util.HashSet<>();

        @Override
        public List<DictItem> items(String dictCode, DictQuery query) {
            calls.incrementAndGet();
            if (emptyCodes.contains(dictCode)) {
                return List.of();
            }
            return List.of(item(dictCode, "v"));
        }

        @Override
        public Map<String, List<DictItem>> batchItems(List<String> dictCodes, DictQuery query) {
            Map<String, List<DictItem>> map = new java.util.LinkedHashMap<>();
            for (String code : dictCodes) {
                map.put(code, items(code, query));
            }
            return map;
        }
    }
}
