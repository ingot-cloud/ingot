package com.ingot.framework.security.provider.service;

/**
 * <p>Description  : AuthorizeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 14:07.</p>
 */
public interface PreAuthorizeService {

    /**
     * 判断接口是否还有角色
     * @param role 角色
     * @return Boolean
     */
    boolean hasRole(String role);

    /**
     * 判断接口是否有任一角色
     * @param roles 角色
     * @return Boolean
     */
    boolean hasAnyRole(String... roles);
}
