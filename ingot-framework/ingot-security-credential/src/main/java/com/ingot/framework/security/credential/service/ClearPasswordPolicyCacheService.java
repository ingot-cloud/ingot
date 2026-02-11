package com.ingot.framework.security.credential.service;

import com.ingot.framework.commons.constants.CacheConstants;

/**
 * ClearPasswordPolicyCacheService
 *
 * @author jy
 * @since 2026/2/11
 */
public interface ClearPasswordPolicyCacheService {
    /**
     * 缓存 KEY
     */
    String CACHE_NAME = CacheConstants.IGNORE_TENANT_PREFIX + ":credential:policies";

    /**
     * 清除缓存
     */
    void evict();
}
