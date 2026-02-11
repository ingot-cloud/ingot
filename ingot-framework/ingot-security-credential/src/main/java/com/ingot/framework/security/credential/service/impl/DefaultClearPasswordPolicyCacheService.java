package com.ingot.framework.security.credential.service.impl;

import com.ingot.framework.core.config.LocalCacheConfig;
import com.ingot.framework.security.credential.service.ClearPasswordPolicyCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;

/**
 * DefaultClearPasswordPolicyCacheService
 *
 * @author jy
 * @since 2026/2/11
 */
@Slf4j
public class DefaultClearPasswordPolicyCacheService implements ClearPasswordPolicyCacheService {

    @Override
    @CacheEvict(
            value = CACHE_NAME,
            allEntries = true,
            cacheManager = LocalCacheConfig.CACHE_MANAGER
    )
    public void evict() {
        log.info("Evicting all entries from cache: {}", CACHE_NAME);
    }
}
