package com.ingot.framework.security.provider.service;

/**
 * <p>Description  : JwtKeyService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 11:40.</p>
 */
public interface JwtKeyService {

    /**
     * 获取 jwt public key
     * @return key
     */
    String fetch();

    /**
     * 从缓存中获取 jwt public key
     * @return key
     */
    String fetchFromCache();
}
