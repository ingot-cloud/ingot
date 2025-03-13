package com.ingot.framework.security.core.tenantdetails;

import com.ingot.framework.core.model.security.TenantDetailsRequest;
import com.ingot.framework.core.model.security.TenantDetailsResponse;
import com.ingot.framework.core.model.support.R;

/**
 * <p>Description  : RemoteTenantDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:54 PM.</p>
 */
public interface RemoteTenantDetailsService {

    /**
     * 获取允许列表
     *
     * @param params {@link TenantDetailsRequest} 请求参数
     * @return {@link TenantDetailsResponse}
     */
    R<TenantDetailsResponse> getAllowList(TenantDetailsRequest params);
}
