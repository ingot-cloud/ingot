package com.ingot.framework.security.access.service.impl;

import com.ingot.framework.security.access.internal.LocalLoginFailureFloorSupplier;
import com.ingot.framework.security.access.internal.LoginFailureLkgStore;
import com.ingot.framework.security.access.internal.LoginFailurePolicyRemoteUnavailableException;
import com.ingot.framework.security.access.internal.LoginFailurePolicySource;
import com.ingot.framework.security.access.internal.LoginFailurePolicySourceHolder;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 弹性登录失败策略加载器：remote → LKG → Nacos 地板。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class ResilientLoginFailurePolicyLoader implements LoginFailurePolicyLoader {

    private final RemoteLoginFailurePolicyLoader delegate;
    private final LoginFailureLkgStore lkgStore;
    private final LocalLoginFailureFloorSupplier floorSupplier;
    private final boolean localFloorEnabled;
    private final LoginFailurePolicySourceHolder sourceHolder;

    public ResilientLoginFailurePolicyLoader(RemoteLoginFailurePolicyLoader delegate,
                                             LoginFailureLkgStore lkgStore,
                                             LocalLoginFailureFloorSupplier floorSupplier,
                                             boolean localFloorEnabled,
                                             LoginFailurePolicySourceHolder sourceHolder) {
        this.delegate = delegate;
        this.lkgStore = lkgStore;
        this.floorSupplier = floorSupplier;
        this.localFloorEnabled = localFloorEnabled;
        this.sourceHolder = sourceHolder;
    }

    @Override
    public List<LoginFailurePolicy> loadAll() {
        try {
            List<LoginFailurePolicy> data = delegate.loadAll();
            lkgStore.save(data);
            sourceHolder.mark(LoginFailurePolicySource.REMOTE);
            return data;
        } catch (LoginFailurePolicyRemoteUnavailableException e) {
            return fallback(e);
        }
    }

    private List<LoginFailurePolicy> fallback(LoginFailurePolicyRemoteUnavailableException cause) {
        List<LoginFailurePolicy> lkg = lkgStore.load();
        if (lkg != null) {
            sourceHolder.mark(LoginFailurePolicySource.LAST_KNOWN_GOOD);
            log.warn("[LoginFailure] remote unavailable, using LKG, size={}, cause={}",
                    lkg.size(), cause.getMessage());
            return lkg;
        }
        if (!localFloorEnabled) {
            log.error("[LoginFailure] remote unavailable, no LKG, local-floor disabled", cause);
            throw cause;
        }
        List<LoginFailurePolicy> floor = floorSupplier.get();
        sourceHolder.mark(LoginFailurePolicySource.LOCAL_FLOOR);
        log.warn("[LoginFailure] remote unavailable, using local floor, size={}, cause={}",
                floor.size(), cause.getMessage());
        return floor;
    }
}
