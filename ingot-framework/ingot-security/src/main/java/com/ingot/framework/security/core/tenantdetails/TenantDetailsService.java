package com.ingot.framework.security.core.tenantdetails;

/**
 * <p>Description  : TenantDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:51 PM.</p>
 */
public interface TenantDetailsService {

    /**
     * 根据用户名获取该用户可以访问的租户详情
     *
     * @param username 用户名
     * @return {@link TenantDetails}
     */
    TenantDetails loadByUsername(String username);
}
