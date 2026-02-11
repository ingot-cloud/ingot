package com.ingot.framework.security.credential.service.impl;

import com.ingot.framework.security.credential.service.ClearCredentialPolicyConfigCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;

/**
 * DefaultPolicyConfigCacheService
 *
 * @author jy
 * @since 2026/2/9
 */
@Slf4j
public class DefaultClearCredentialPolicyConfigCacheService implements ClearCredentialPolicyConfigCacheService {

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void evict() {
        log.info("清空所有策略缓存");
    }
}
