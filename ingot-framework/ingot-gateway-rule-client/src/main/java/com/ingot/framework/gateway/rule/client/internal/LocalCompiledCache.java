package com.ingot.framework.gateway.rule.client.internal;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 编译后规则对象的进程内 L1 缓存。
 *
 * <p>安全策略通常需要预编译为可执行结构（{@code PathPattern}、排序后的策略列表、
 * IP/CIDR 索引等），这些对象不可序列化进 Redis，因此只在进程内持有完整快照。</p>
 *
 * <p>语义：{@link AtomicReference} 要么持有完整编译结果，要么为 null（无 TTL / size 上限）。
 * cache miss 时通过 {@link #get(Supplier)} 的 loader 同步加载；跨节点变更由
 * {@link SecurityPolicyCacheCoordinator} 触发 {@link #evictAll()}。</p>
 *
 * <p>被限流 / 黑白名单 / 挑战各域 Service 内部持有，不对外暴露为 Spring Bean。</p>
 *
 * @param <T> 编译后类型（如 {@code RateLimitSnapshot}、{@code CompiledIpList}）
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
