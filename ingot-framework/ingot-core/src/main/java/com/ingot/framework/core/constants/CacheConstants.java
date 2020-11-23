package com.ingot.framework.core.constants;

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
    String NAME_PREFIX = "INGOT:CACHE:";

    /**
     * OAuth 客户端缓存 key
     */
    String CLIENT_DETAILS_KEY = NAME_PREFIX + "CLIENT_DETAILS";
}
