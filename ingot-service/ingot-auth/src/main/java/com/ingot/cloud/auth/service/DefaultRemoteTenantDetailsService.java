package com.ingot.cloud.auth.service;

import com.ingot.cloud.pms.api.rpc.PmsUserAuthFeignApi;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.security.core.tenantdetails.RemoteTenantDetailsService;
import com.ingot.framework.security.core.tenantdetails.TenantDetailsRequest;
import com.ingot.framework.security.core.tenantdetails.TenantDetailsResponse;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultRemoteTenantDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 4:29 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultRemoteTenantDetailsService implements RemoteTenantDetailsService {
    private final PmsUserAuthFeignApi pmsUserAuthFeignApi;

    @Override
    public R<TenantDetailsResponse> getAllowList(TenantDetailsRequest params) {
        return pmsUserAuthFeignApi.getUserTenantDetails(params.getUsername());
    }
}
