package com.ingot.framework.security.credential.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.ingot.framework.security.credential.policy.PasswordPolicy;

/**
 * 编译后的密码策略列表本地缓存。
 * <p>
 * 因为编译后的 {@link PasswordPolicy} 持有 {@code PasswordEncoder} 等 Spring Bean，无法序列化进 Redis，
 * 因此只在每个进程内做 L1 缓存；通过 {@code CredentialCacheCoordinator} 订阅
 * {@code CredentialInvalidationEvent} 在跨节点变更时同步置空，下一次 {@link #get(Supplier)} 将
 * 重新从下层装饰器拿到 DTO 并 compile。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
public class LocalCompiledPolicyCache {

    private final AtomicReference<List<PasswordPolicy>> ref = new AtomicReference<>();

    /**
     * 获取编译后的策略列表；为空时调用 {@code loader} 重新加载并写入。
     * <p>实现采用 double-check：避免空时多线程重复编译。</p>
     */
    public List<PasswordPolicy> get(Supplier<List<PasswordPolicy>> loader) {
        List<PasswordPolicy> cached = ref.get();
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = ref.get();
            if (cached != null) {
                return cached;
            }
            List<PasswordPolicy> fresh = loader.get();
            if (fresh == null) {
                fresh = List.of();
            }
            ref.set(fresh);
            return fresh;
        }
    }

    /**
     * 失效本地编译结果，下次 {@link #get(Supplier)} 时重新加载。
     */
    public void evictAll() {
        ref.set(null);
    }
}
