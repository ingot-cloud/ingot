package com.ingot.framework.security.account.adapter.policy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.config.AccountDomainProperties;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.service.AccountLockoutPolicyLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>账号锁定策略分层缓存：L1 命中、evictAll、按 userType 取行、地板与 fail-closed。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CachedAccountLockoutPolicyLoaderTest {

    private static LockoutPolicy adminPolicy() {
        return new LockoutPolicy(UserTypeEnum.ADMIN, true, 5, 30, 15, 3);
    }

    private static LockoutPolicy appPolicy() {
        return new LockoutPolicy(UserTypeEnum.APP, true, 5, 15, 15, 3);
    }

    private LayeredCache<String, List<LockoutPolicy>> cache(
            CacheValueLoader<String, List<LockoutPolicy>> loader,
            CacheSourceHolder holder,
            boolean localFloorEnabled) {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(true)
                .l2Enabled(false)
                .resilienceEnabled(true)
                .localFloorEnabled(localFloorEnabled)
                .build();
        return LayeredCacheBuilder.<String, List<LockoutPolicy>>named("account-lockout-policy")
                .loader(loader)
                .settings(settings)
                .cacheable(v -> v != null && !v.isEmpty())
                .emptyValue(List::of)
                .sourceHolder(holder)
                .resilient(null, new LocalAccountLockoutFloorSupplier(new AccountDomainProperties()))
                .build();
    }

    @Test
    @DisplayName("按 userType 取对应行")
    void pickByUserType() {
        CacheValueCountingLoader loader = new CacheValueCountingLoader(List.of(adminPolicy(), appPolicy()));
        AccountLockoutPolicyLoader policyLoader = new CachedAccountLockoutPolicyLoader(
                cache(loader, new CacheSourceHolder(), true));

        assertThat(policyLoader.getLockoutPolicy(UserTypeEnum.APP).lockDurationMinutes()).isEqualTo(15);
        assertThat(policyLoader.getLockoutPolicy(UserTypeEnum.ADMIN).lockDurationMinutes()).isEqualTo(30);
        assertThat(loader.calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("缺行时回落快照第一行（地板单元素）")
    void missingRowFallsBackToFirst() {
        CacheValueCountingLoader loader = new CacheValueCountingLoader(List.of(adminPolicy()));
        AccountLockoutPolicyLoader policyLoader = new CachedAccountLockoutPolicyLoader(
                cache(loader, new CacheSourceHolder(), true));

        LockoutPolicy result = policyLoader.getLockoutPolicy(UserTypeEnum.APP);
        assertThat(result.userType()).isEqualTo(UserTypeEnum.ADMIN);
        assertThat(result.maxAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("evictAll 真实清除 L1，下次读取重新穿透")
    void evictAllClearsL1() {
        CacheValueCountingLoader loader = new CacheValueCountingLoader(List.of(adminPolicy()));
        AccountLockoutPolicyLoader policyLoader = new CachedAccountLockoutPolicyLoader(
                cache(loader, new CacheSourceHolder(), true));

        policyLoader.getLockoutPolicy(UserTypeEnum.ADMIN);
        policyLoader.evictAll();
        policyLoader.getLockoutPolicy(UserTypeEnum.ADMIN);

        assertThat(loader.calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("远端失败且无 LKG 时使用 Nacos 地板")
    void remoteFailureUsesFloor() {
        CacheSourceHolder holder = new CacheSourceHolder();
        AccountLockoutPolicyLoader policyLoader = new CachedAccountLockoutPolicyLoader(
                cache(CacheValueCountingLoader.unavailable(), holder, true));

        LockoutPolicy result = policyLoader.getLockoutPolicy(UserTypeEnum.APP);

        assertThat(result.maxAttempts()).isEqualTo(5);
        assertThat(holder.current()).isEqualTo(CacheSource.LOCAL_FLOOR);
        assertThat(holder.localFloorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("远端失败、无 LKG 且地板关闭时 fail-closed 抛异常")
    void remoteFailureFloorDisabledThrows() {
        AccountLockoutPolicyLoader policyLoader = new CachedAccountLockoutPolicyLoader(
                cache(CacheValueCountingLoader.unavailable(), new CacheSourceHolder(), false));

        assertThatThrownBy(() -> policyLoader.getLockoutPolicy(UserTypeEnum.ADMIN))
                .isInstanceOf(AccountLockoutPolicyRemoteUnavailableException.class);
    }

    private static final class CacheValueCountingLoader
            implements CacheValueLoader<String, List<LockoutPolicy>> {

        private final AtomicInteger calls = new AtomicInteger();
        private final List<LockoutPolicy> value;
        private final boolean unavailable;

        private CacheValueCountingLoader(List<LockoutPolicy> value) {
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
        public List<LockoutPolicy> load(String key) {
            calls.incrementAndGet();
            if (unavailable) {
                throw new AccountLockoutPolicyRemoteUnavailableException("boom");
            }
            return value;
        }
    }
}
