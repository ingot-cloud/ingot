package com.ingot.framework.security.credential;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.internal.CredentialRemoteUnavailableException;
import com.ingot.framework.security.credential.internal.LayeredCredentialPolicyConfigService;
import com.ingot.framework.security.credential.internal.LocalFloorSupplier;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>凭证策略分层缓存的降级阶梯：远端成功、合法空、地板、fail-closed。</p>
 *
 * <p>LKG 路径由框架 {@code ResilientCacheLayer} 单测覆盖；本测试验证凭证地板与异常类型接入正确。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class LayeredCredentialPolicyConfigServiceTest {

    private final LocalFloorSupplier floor = new LocalFloorSupplier(new CredentialSecurityProperties());
    private final CacheSourceHolder holder = new CacheSourceHolder();

    private static CredentialPolicyConfigVO vo(String type) {
        CredentialPolicyConfigVO v = new CredentialPolicyConfigVO();
        v.setPolicyType(type);
        return v;
    }

    private CredentialPolicyConfigService service(CredentialPolicyConfigService delegate,
                                                  boolean localFloorEnabled) {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(false)
                .l2Enabled(false)
                .resilienceEnabled(true)
                .localFloorEnabled(localFloorEnabled)
                .build();
        LayeredCache<String, List<CredentialPolicyConfigVO>> cache =
                LayeredCacheBuilder.<String, List<CredentialPolicyConfigVO>>named("credential")
                        .loader(key -> delegate.getAll())
                        .settings(settings)
                        .cacheable(v -> v != null && !v.isEmpty())
                        .emptyValue(List::of)
                        .sourceHolder(holder)
                        .resilient(null, floor)
                        .build();
        return new LayeredCredentialPolicyConfigService(cache);
    }

    @Test
    void success_marksRemote() {
        List<CredentialPolicyConfigVO> data = List.of(vo("1"));
        CredentialPolicyConfigService svc = service(() -> data, true);

        List<CredentialPolicyConfigVO> result = svc.getAll();

        assertEquals(1, result.size());
        assertEquals(CacheSource.REMOTE, holder.current());
    }

    @Test
    void legalEmpty_acceptedNoFallback() {
        CredentialPolicyConfigService svc = service(List::of, true);

        List<CredentialPolicyConfigVO> result = svc.getAll();

        assertTrue(result.isEmpty());
        assertEquals(CacheSource.REMOTE, holder.current());
        assertEquals(0, holder.lastKnownGoodCount());
        assertEquals(0, holder.localFloorCount());
    }

    @Test
    void failure_noLkg_usesLocalFloor() {
        CredentialPolicyConfigService svc = service(() -> {
            throw new CredentialRemoteUnavailableException("boom");
        }, true);

        List<CredentialPolicyConfigVO> result = svc.getAll();

        assertFalse(result.isEmpty(), "地板必须非空，避免 fail-open");
        assertEquals(CacheSource.LOCAL_FLOOR, holder.current());
        assertEquals(1, holder.localFloorCount());
    }

    @Test
    void failure_noLkg_floorDisabled_throws() {
        CredentialPolicyConfigService svc = service(() -> {
            throw new CredentialRemoteUnavailableException("boom");
        }, false);

        assertThrows(CredentialRemoteUnavailableException.class, svc::getAll);
    }

    @Test
    void evictAll_allowsReload() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        CredentialPolicyConfigService delegate = () -> {
            calls.incrementAndGet();
            return List.of(vo("1"));
        };
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(true)
                .l2Enabled(false)
                .resilienceEnabled(false)
                .build();
        LayeredCache<String, List<CredentialPolicyConfigVO>> cache =
                LayeredCacheBuilder.<String, List<CredentialPolicyConfigVO>>named("credential")
                        .loader(key -> delegate.getAll())
                        .settings(settings)
                        .cacheable(v -> v != null && !v.isEmpty())
                        .emptyValue(List::of)
                        .build();
        CredentialPolicyConfigService svc = new LayeredCredentialPolicyConfigService(cache);

        svc.getAll();
        svc.getAll();
        svc.evictAll();
        svc.getAll();

        assertEquals(2, calls.get());
    }
}
