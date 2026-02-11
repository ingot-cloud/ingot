package com.ingot.framework.security.credential.service;

import com.ingot.framework.commons.constants.CacheConstants;

/**
 * PolicyConfigCacheService
 *
 * @author jy
 * @since 2026/2/9
 */
public interface ClearCredentialPolicyConfigCacheService {
    /**
     * 缓存 KEY
     */
    String CACHE_NAME = CacheConstants.IGNORE_TENANT_PREFIX + ":credential:configs";

    /**
     * 清空所有缓存
     */
    void evict();
}
