package com.ingot.framework.security.credential.internal;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.model.CredentialPolicySource;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.extern.slf4j.Slf4j;

/**
 * 弹性降级装饰器：在原始远程 delegate 之下提供 {@code remote(新鲜) → LKG → Nacos 地板} 的降级阶梯，
 * 保证安全中心抖动 / 宕机期间绝不 fail-open 到「无策略」。
 *
 * <p>装配位置为热缓存链（L1 Caffeine / L2 Redis）之下、原始 {@code RemoteCredentialPolicyConfigService}
 * 之上，即成为热缓存的最内层 delegate。行为：</p>
 * <ul>
 *   <li>远程成功（含合法空）：刷新 LKG 并返回，标记来源 {@link CredentialPolicySource#REMOTE}。</li>
 *   <li>远程失败（{@link CredentialRemoteUnavailableException}）：有 LKG 用 LKG（{@link CredentialPolicySource#LAST_KNOWN_GOOD}），
 *       否则落 Nacos 地板（{@link CredentialPolicySource#LOCAL_FLOOR}）；{@code localFloorEnabled=false} 且无 LKG 时向上抛出。</li>
 * </ul>
 * <p>失败时向上抛出（无 LKG 且禁用地板）或返回有效兜底值，绝不返回「失败空」；返回的兜底值为有效数据，
 * 允许被上层热缓存短 TTL 缓存，远程恢复后随缓存过期自动回到新鲜值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class ResilientCredentialPolicyConfigService implements CredentialPolicyConfigService {

    private final CredentialPolicyConfigService delegate;
    private final LastKnownGoodStore lkgStore;
    private final LocalFloorSupplier localFloorSupplier;
    private final boolean localFloorEnabled;
    private final CredentialPolicySourceHolder sourceHolder;

    public ResilientCredentialPolicyConfigService(CredentialPolicyConfigService delegate,
                                                  LastKnownGoodStore lkgStore,
                                                  LocalFloorSupplier localFloorSupplier,
                                                  boolean localFloorEnabled,
                                                  CredentialPolicySourceHolder sourceHolder) {
        this.delegate = delegate;
        this.lkgStore = lkgStore;
        this.localFloorSupplier = localFloorSupplier;
        this.localFloorEnabled = localFloorEnabled;
        this.sourceHolder = sourceHolder;
    }

    @Override
    public List<CredentialPolicyConfigVO> getAll() {
        try {
            List<CredentialPolicyConfigVO> data = delegate.getAll();
            // 成功（含合法空）：刷新 LKG 并回到正常来源。
            lkgStore.save(data);
            sourceHolder.mark(CredentialPolicySource.REMOTE);
            return data != null ? data : List.of();
        } catch (CredentialRemoteUnavailableException e) {
            return fallback(e);
        }
    }

    private List<CredentialPolicyConfigVO> fallback(CredentialRemoteUnavailableException cause) {
        List<CredentialPolicyConfigVO> lkg = lkgStore.load();
        if (lkg != null) {
            sourceHolder.mark(CredentialPolicySource.LAST_KNOWN_GOOD);
            log.warn("[Credential] 远程策略不可用，降级使用最近成功快照(LKG)，size={}, cause={}",
                    lkg.size(), cause.getMessage());
            return lkg;
        }
        if (!localFloorEnabled) {
            log.error("[Credential] 远程策略不可用且无 LKG，local-floor-enabled=false，向上抛出", cause);
            throw cause;
        }
        List<CredentialPolicyConfigVO> floor = localFloorSupplier.get();
        sourceHolder.mark(CredentialPolicySource.LOCAL_FLOOR);
        log.warn("[Credential] 远程策略不可用且无 LKG，降级使用 Nacos 本地地板，size={}, cause={}",
                floor.size(), cause.getMessage());
        return floor;
    }

    @Override
    public void evictAll() {
        // 仅向下透传热缓存清理；LKG 生命周期独立，不随失效事件清除。
        delegate.evictAll();
    }
}
