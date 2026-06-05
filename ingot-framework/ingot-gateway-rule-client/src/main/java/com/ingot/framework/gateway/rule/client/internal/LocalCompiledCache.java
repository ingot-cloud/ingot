package com.ingot.framework.gateway.rule.client.internal;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 编译后规则对象的本地缓存（进程内 L1）。
 *
 * <p>规则通常需要预编译为可执行结构（如 {@code PathPattern} / 排序后的列表 / IP 段索引等），
 * 这些对象常持有 Spring Bean 引用、不可序列化进 Redis，因此只在进程内做 L1，
 * 跨节点变更通过 {@code SecurityPolicyCacheCoordinator} 订阅事件统一 evict。</p>
 *
 * @param <T> 编译后类型
 * @author jy
 * @since 2026/5/26
 */
public class LocalCompiledCache<T> {

    private final AtomicReference<T> ref = new AtomicReference<>();

    /**
     * 获取编译结果；为空时调用 {@code loader} 重新加载并写入。
     */
    public T get(Supplier<T> loader) {
        T cached = ref.get();
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = ref.get();
            if (cached != null) {
                return cached;
            }
            T fresh = loader.get();
            ref.set(fresh);
            return fresh;
        }
    }

    /**
     * 当前已编译结果（不触发加载，可能为 null）。
     */
    public T peek() {
        return ref.get();
    }

    /**
     * 失效本地编译结果，下次 {@link #get} 将重新加载。
     */
    public void evictAll() {
        ref.set(null);
    }
}
