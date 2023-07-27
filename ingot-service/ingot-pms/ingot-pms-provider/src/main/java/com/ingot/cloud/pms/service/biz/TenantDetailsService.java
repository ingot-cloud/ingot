package com.ingot.cloud.pms.service.biz;

import com.ingot.framework.security.core.tenantdetails.TenantDetailsResponse;

/**
 * <p>Description  : TenantDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 4:37 PM.</p>
 */
public interface TenantDetailsService {

    /**
     * 获取用户租户详情
     *
     * @param username 用户名
     * @return {@link TenantDetailsResponse}
     */
    TenantDetailsResponse getUserTenantDetails(String username);
}
