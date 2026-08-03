package com.ingot.framework.cache.derived;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * <p>由共享快照编译而来的派生对象缓存，以 {@link SnapshotVersion} 判定是否需要重新编译。</p>
 *
 * <p>存在的前提是编译产物往往<b>不可序列化</b>——正则 {@code Pattern}、{@code PathPattern}、
 * 预建索引等只能留在本机，无法进 Redis。因此派生层不参与分层缓存，只在进程内按版本复用。</p>
 *
 * <p>版本未变化时直接复用旧产物，既省去重复编译，也避免下游（如 Sentinel 规则加载）被无谓地反复刷新。
 * 上游共享快照的 TTL 到期重新拉取时，只要版本没变，这里就不会有任何动作。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * VersionedDerivedCache<SecurityPolicySnapshotVO, CompiledIpList> cache =
 *         new VersionedDerivedCache<>(
 *                 vo -> new SnapshotVersion(sourceHolder.current(), vo.getVersion()),
 *                 vo -> CompiledIpList.compile(toItems(vo)));
 * CompiledIpList compiled = cache.get(sharedCache.get(KEY));
 * }</pre>
 *
 * @param <S> 源快照类型
 * @param <D> 派生产物类型
 * @author jy
 * @since 1.0.0
 * @implNote 采用双重检查加载，同一进程内并发 miss 时只编译一次。
 */
public class VersionedDerivedCache<S, D> {

    private final Function<S, SnapshotVersion> versionExtractor;
    private final Function<S, D> compiler;
    private final AtomicReference<Entry<D>> ref = new AtomicReference<>();

    /**
     * @param versionExtractor 从源快照提取失效键
     * @param compiler         源快照到派生产物的编译逻辑
     */
    public VersionedDerivedCache(Function<S, SnapshotVersion> versionExtractor,
                                 Function<S, D> compiler) {
        this.versionExtractor = versionExtractor;
        this.compiler = compiler;
    }

    /**
     * 获取派生产物；源快照版本与上次一致时复用，否则重新编译。
     *
     * @param source 源快照
     * @return 派生产物
     */
    public D get(S source) {
        SnapshotVersion version = versionExtractor.apply(source);
        Entry<D> cached = ref.get();
        if (cached != null && cached.version().equals(version)) {
            return cached.value();
        }
        synchronized (this) {
            cached = ref.get();
            if (cached != null && cached.version().equals(version)) {
                return cached.value();
            }
            D compiled = compiler.apply(source);
            ref.set(new Entry<>(version, compiled));
            return compiled;
        }
    }

    /**
     * 当前已编译产物对应的版本；尚未编译时为 {@code null}。
     *
     * @return 版本键
     */
    public SnapshotVersion currentVersion() {
        Entry<D> cached = ref.get();
        return cached == null ? null : cached.version();
    }

    /**
     * 丢弃已编译产物，下次 {@link #get} 强制重新编译。
     */
    public void evictAll() {
        ref.set(null);
    }

    private record Entry<D>(SnapshotVersion version, D value) {
    }
}
