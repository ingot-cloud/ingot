package com.ingot.framework.crypto.web;

import com.ingot.framework.crypto.model.CryptoType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>Description  : SecretKeyResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 11:05 AM.</p>
 */
public interface SecretKeyResolver {

    /**
     * 获取秘钥
     *
     * @param request {@link HttpServletRequest}
     * @param type    {@link CryptoType}
     * @return 秘钥
     */
    String get(HttpServletRequest request, CryptoType type);
}
