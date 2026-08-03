package com.ingot.framework.cache.derived;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * <p>没有版本源的派生对象缓存，只在显式失效后才重新编译。</p>
 *
 * <p>适用于数据来自本地配置的场景：源是进程内的 {@code Properties} 对象，没有可比对的版本号，
 * 变更只能通过失效事件或重启告知。这是 {@link VersionedDerivedCache} 的退化形式——
 * 有版本号可比时应优先使用后者，它能在上游 TTL 刷新但内容未变时避免重复编译。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * LazyDerivedCache<CompiledIpList> cache =
 *         new LazyDerivedCache<>(() -> CompiledIpList.compile(properties.getPolicy().getItems()));
 * boolean blocked = cache.get().isBlocked(ip);
 * }</pre>
 *
 * @param <D> 派生产物类型
 * @author jy
 * @since 1.0.0
 * @see VersionedDerivedCache
 * @implNote 采用双重检查加载，并发 miss 时只编译一次。
 */
public class LazyDerivedCache<D> {

    private final Supplier<D> compiler;
    private final AtomicReference<D> ref = new AtomicReference<>();

    /**
     * @param compiler 派生产物的编译逻辑
     */
    public LazyDerivedCache(Supplier<D> compiler) {
        this.compiler = compiler;
    }

    /**
     * 获取派生产物；尚未编译或已失效时同步编译。
     *
     * @return 派生产物
     */
    public D get() {
        D cached = ref.get();
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = ref.get();
            if (cached != null) {
                return cached;
            }
            D compiled = compiler.get();
            ref.set(compiled);
            return compiled;
        }
    }

    /**
     * 当前已编译产物；不触发编译，可能为 {@code null}。
     *
     * @return 派生产物或 {@code null}
     */
    public D peek() {
        return ref.get();
    }

    /**
     * 丢弃已编译产物，下次 {@link #get()} 强制重新编译。
     */
    public void evictAll() {
        ref.set(null);
    }
}
