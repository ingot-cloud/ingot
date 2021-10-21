package com.ingot.framework.core.constants;

import static com.ingot.framework.core.constants.RedisConstants.BASE_PREFIX;

/**
 * <p>Description  : CacheConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-20.</p>
 * <p>Time         : 15:27.</p>
 */
public interface CacheConstants {

    /**
     * cache name 前缀
     */
    String NAME_PREFIX = BASE_PREFIX + "cache:";

    /**
     * OAuth 客户端缓存 key
     */
    String REGISTERED_CLIENT_KEY = NAME_PREFIX + "client";
}
