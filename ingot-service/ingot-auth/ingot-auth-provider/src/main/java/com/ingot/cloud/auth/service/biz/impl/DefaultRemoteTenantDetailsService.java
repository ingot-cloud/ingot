package com.ingot.cloud.auth.service.biz.impl;

import com.ingot.cloud.iam.api.rpc.RemoteIamTenantDetailsService;
import com.ingot.framework.commons.model.security.TenantDetailsRequest;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.tenantdetails.RemoteTenantDetailsService;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultRemoteTenantDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 4:29 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultRemoteTenantDetailsService implements RemoteTenantDetailsService {
    private final RemoteIamTenantDetailsService remoteIamTenantDetailsService;

    @Override
    public R<TenantDetailsResponse> getAllowList(TenantDetailsRequest params) {
        return remoteIamTenantDetailsService.getUserTenantDetails(params.getUsername());
    }
}
