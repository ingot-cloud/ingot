package com.ingot.framework.security.credential.internal;

import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.security.credential.event.CredentialInvalidationEvent;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;

/**
 * <p>凭证策略失效广播的订阅入口，把通用协调器固化到本模块的事件类型上。</p>
 *
 * <p>凭证失效事件没有域维度，一律全量清理 L1+L2。发布方收不到自身广播，写侧必须自行清本地。</p>
 *
 * @author jy
 * @since 2026/5/16
 * @see LayeredCacheCoordinator
 */
public class CredentialCacheCoordinator
        extends LayeredCacheCoordinator<CredentialInvalidationEvent, String> {

    /**
     * 凭证策略只有全量失效，域标识无业务语义。
     */
    private static final String ALL = "all";

    public CredentialCacheCoordinator(InvalidationBus bus,
                                      CredentialPolicyConfigService policyConfigService) {
        super(bus, CredentialInvalidationEvent.class, event -> ALL, ALL);
        register(ALL, policyConfigService::evictAll);
    }
}
