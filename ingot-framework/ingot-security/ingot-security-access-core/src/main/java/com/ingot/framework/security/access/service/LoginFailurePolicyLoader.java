package com.ingot.framework.security.access.service;

import com.ingot.framework.security.access.model.LoginFailurePolicy;

import java.util.List;

/**
 * 登录失败保护策略加载器。
 *
 * @author jy
 * @since 1.0.0
 */
public interface LoginFailurePolicyLoader {

    List<LoginFailurePolicy> loadAll();

    default void evictAll() {
        // 默认无缓存
    }
}
