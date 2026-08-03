package com.ingot.framework.cache.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mockito.stubbing.Answer;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>由 {@link HashMap} 支撑的 {@link StringRedisTemplate} 测试替身，覆盖读写、删除与 SCAN。</p>
 *
 * <p>SCAN 只支持末尾通配的前缀匹配，足以验证多 key 场景的批量清理；不追求完整 glob 语义。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class FakeRedis {

    private final Map<String, String> store = new HashMap<>();
    private final StringRedisTemplate template;

    @SuppressWarnings("unchecked")
    public FakeRedis() {
        this.template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);

        when(ops.get(anyString())).thenAnswer(inv -> store.get(inv.<String>getArgument(0)));

        Answer<Void> set = inv -> {
            store.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        };
        doAnswer(set).when(ops).set(anyString(), anyString());
        doAnswer(set).when(ops).set(anyString(), anyString(), any(Duration.class));

        when(template.delete(anyString())).thenAnswer(inv -> store.remove(inv.<String>getArgument(0)) != null);
        when(template.delete(anyCollection())).thenAnswer(inv -> {
            Collection<String> keys = inv.getArgument(0);
            long removed = 0;
            for (String key : keys) {
                if (store.remove(key) != null) {
                    removed++;
                }
            }
            return removed;
        });

        when(template.scan(any(ScanOptions.class))).thenAnswer(inv -> {
            ScanOptions options = inv.getArgument(0);
            String pattern = options.getPattern();
            String prefix = pattern == null ? "" : pattern.replace("*", "");
            List<String> matched = new ArrayList<>();
            for (String key : store.keySet()) {
                if (key.startsWith(prefix)) {
                    matched.add(key);
                }
            }
            Iterator<String> it = matched.iterator();
            Cursor<String> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenAnswer(i -> it.hasNext());
            when(cursor.next()).thenAnswer(i -> it.next());
            return cursor;
        });
    }

    public StringRedisTemplate template() {
        return template;
    }

    public Map<String, String> store() {
        return store;
    }

    public String get(String key) {
        return store.get(key);
    }

    public boolean has(String key) {
        return store.containsKey(key);
    }

    public int size() {
        return store.size();
    }
}
