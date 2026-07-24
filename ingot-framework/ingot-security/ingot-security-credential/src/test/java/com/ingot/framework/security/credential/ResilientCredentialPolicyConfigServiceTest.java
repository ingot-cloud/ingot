package com.ingot.framework.security.credential;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.internal.CredentialPolicySourceHolder;
import com.ingot.framework.security.credential.internal.CredentialRemoteUnavailableException;
import com.ingot.framework.security.credential.internal.LastKnownGoodStore;
import com.ingot.framework.security.credential.internal.LocalFloorSupplier;
import com.ingot.framework.security.credential.internal.ResilientCredentialPolicyConfigService;
import com.ingot.framework.security.credential.model.CredentialPolicySource;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ResilientCredentialPolicyConfigService} 降级阶梯单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class ResilientCredentialPolicyConfigServiceTest {

    /**
     * 测试替身：以进程内 map 承载 LKG，替代真实 Redis。
     * <p>生产实现 {@link LastKnownGoodStore} 已移除进程内副本、仅依赖 Redis；此处仅为在无 Redis 的
     * 单元测试中驱动"LKG 有/无"两种分支，验证 Resilient 的降级阶梯逻辑。</p>
     */
    private final List<List<CredentialPolicyConfigVO>> lkgHolder = new java.util.ArrayList<>(1);
    private final LastKnownGoodStore lkg = new LastKnownGoodStore(null, null, "test:lkg", null) {
        @Override
        public void save(List<CredentialPolicyConfigVO> data) {
            lkgHolder.clear();
            lkgHolder.add(data != null ? data : List.of());
        }

        @Override
        public List<CredentialPolicyConfigVO> load() {
            return lkgHolder.isEmpty() ? null : lkgHolder.get(0);
        }
    };
    private final LocalFloorSupplier floor = new LocalFloorSupplier(new CredentialSecurityProperties());
    private final CredentialPolicySourceHolder holder = new CredentialPolicySourceHolder();

    private static CredentialPolicyConfigVO vo(String type) {
        CredentialPolicyConfigVO v = new CredentialPolicyConfigVO();
        v.setPolicyType(type);
        return v;
    }

    private ResilientCredentialPolicyConfigService resilient(CredentialPolicyConfigService delegate,
                                                             boolean localFloorEnabled) {
        return new ResilientCredentialPolicyConfigService(delegate, lkg, floor, localFloorEnabled, holder);
    }

    @Test
    void success_refreshesLkgAndMarksRemote() {
        List<CredentialPolicyConfigVO> data = List.of(vo("1"));
        ResilientCredentialPolicyConfigService service = resilient(() -> data, true);

        List<CredentialPolicyConfigVO> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals(CredentialPolicySource.REMOTE, holder.current());
        // LKG 已刷新
        assertEquals(1, lkg.load().size());
    }

    @Test
    void legalEmpty_acceptedAndRefreshesLkg_noFallback() {
        ResilientCredentialPolicyConfigService service = resilient(List::of, true);

        List<CredentialPolicyConfigVO> result = service.getAll();

        assertTrue(result.isEmpty());
        assertEquals(CredentialPolicySource.REMOTE, holder.current());
        // 合法空也刷新为 LKG（空快照，load 返回非 null 空集合）
        assertTrue(lkg.load().isEmpty());
    }

    @Test
    void failure_withLkg_usesLkg() {
        lkg.save(List.of(vo("1"), vo("2")));
        ResilientCredentialPolicyConfigService service = resilient(() -> {
            throw new CredentialRemoteUnavailableException("boom");
        }, true);

        List<CredentialPolicyConfigVO> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals(CredentialPolicySource.LAST_KNOWN_GOOD, holder.current());
    }

    @Test
    void failure_noLkg_usesLocalFloor() {
        ResilientCredentialPolicyConfigService service = resilient(() -> {
            throw new CredentialRemoteUnavailableException("boom");
        }, true);

        List<CredentialPolicyConfigVO> result = service.getAll();

        assertFalse(result.isEmpty(), "地板必须非空，避免 fail-open");
        assertEquals(CredentialPolicySource.LOCAL_FLOOR, holder.current());
    }

    @Test
    void failure_noLkg_floorDisabled_throws() {
        ResilientCredentialPolicyConfigService service = resilient(() -> {
            throw new CredentialRemoteUnavailableException("boom");
        }, false);

        assertThrows(CredentialRemoteUnavailableException.class, service::getAll);
    }

    @Test
    void evict_delegatesDown() {
        boolean[] evicted = {false};
        CredentialPolicyConfigService delegate = new CredentialPolicyConfigService() {
            @Override
            public List<CredentialPolicyConfigVO> getAll() {
                return List.of();
            }

            @Override
            public void evictAll() {
                evicted[0] = true;
            }
        };
        resilient(delegate, true).evictAll();
        assertSame(true, evicted[0]);
    }
}
