package com.ingot.framework.security.access.service.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.security.access.config.AccessProtectionProperties;
import com.ingot.framework.security.access.internal.LocalLoginFailureFloorSupplier;
import com.ingot.framework.security.access.internal.LoginFailurePolicyRemoteUnavailableException;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>登录失败策略分层缓存行为：L1 命中、失效后真实清除（修复 D-C）、降级阶梯。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CachedLoginFailurePolicyLoaderTest {

    private static LoginFailurePolicy ipPolicy() {
        return new LoginFailurePolicy(LoginFailureDimension.IP, true, 50, 1, 3600, "IP");
    }

    private LayeredCache<String, List<LoginFailurePolicy>> cache(
            CacheValueCountingLoader loader,
            CacheSourceHolder holder,
            boolean localFloorEnabled) {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(true)
                .l2Enabled(false)
                .resilienceEnabled(true)
                .localFloorEnabled(localFloorEnabled)
                .build();
        return LayeredCacheBuilder.<String, List<LoginFailurePolicy>>named("login-failure-policy")
                .loader(loader)
                .settings(settings)
                .cacheable(v -> v != null && !v.isEmpty())
                .emptyValue(List::of)
                .sourceHolder(holder)
                .resilient(null, new LocalLoginFailureFloorSupplier(new AccessProtectionProperties()))
                .build();
    }

    @Test
    @DisplayName("重复读取命中 L1，不再访问远端")
    void repeatedReadsHitL1() {
        CacheValueCountingLoader loader = new CacheValueCountingLoader(List.of(ipPolicy()));
        LoginFailurePolicyLoader policyLoader = new CachedLoginFailurePolicyLoader(
                cache(loader, new CacheSourceHolder(), true));

        policyLoader.loadAll();
        policyLoader.loadAll();

        assertThat(loader.calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("evictAll 真实清除 L1，下次读取重新穿透（修复 D-C）")
    void evictAllClearsL1() {
        CacheValueCountingLoader loader = new CacheValueCountingLoader(List.of(ipPolicy()));
        LoginFailurePolicyLoader policyLoader = new CachedLoginFailurePolicyLoader(
                cache(loader, new CacheSourceHolder(), true));

        policyLoader.loadAll();
        policyLoader.evictAll();
        policyLoader.loadAll();

        assertThat(loader.calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("远端失败且无 LKG 时使用 Nacos 地板")
    void remoteFailureUsesFloor() {
        CacheSourceHolder holder = new CacheSourceHolder();
        CacheValueCountingLoader loader = CacheValueCountingLoader.unavailable();
        LoginFailurePolicyLoader policyLoader = new CachedLoginFailurePolicyLoader(
                cache(loader, holder, true));

        List<LoginFailurePolicy> result = policyLoader.loadAll();

        assertThat(result).isNotEmpty();
        assertThat(holder.current()).isEqualTo(CacheSource.LOCAL_FLOOR);
        assertThat(holder.localFloorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("远端失败、无 LKG 且地板关闭时 fail-closed 抛异常")
    void remoteFailureFloorDisabledThrows() {
        CacheValueCountingLoader loader = CacheValueCountingLoader.unavailable();
        LoginFailurePolicyLoader policyLoader = new CachedLoginFailurePolicyLoader(
                cache(loader, new CacheSourceHolder(), false));

        assertThatThrownBy(policyLoader::loadAll)
                .isInstanceOf(LoginFailurePolicyRemoteUnavailableException.class);
    }

    private static final class CacheValueCountingLoader
            implements com.ingot.framework.cache.spi.CacheValueLoader<String, List<LoginFailurePolicy>> {

        private final AtomicInteger calls = new AtomicInteger();
        private final List<LoginFailurePolicy> value;
        private final boolean unavailable;

        private CacheValueCountingLoader(List<LoginFailurePolicy> value) {
            this.value = value;
            this.unavailable = false;
        }

        private CacheValueCountingLoader(boolean unavailable) {
            this.value = List.of();
            this.unavailable = unavailable;
        }

        static CacheValueCountingLoader unavailable() {
            return new CacheValueCountingLoader(true);
        }

        @Override
        public List<LoginFailurePolicy> load(String key) {
            calls.incrementAndGet();
            if (unavailable) {
                throw new LoginFailurePolicyRemoteUnavailableException("boom");
            }
            return value;
        }
    }
}
