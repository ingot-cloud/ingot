package com.ingot.cloud.iam.application.tenant;

import java.util.List;

import com.ingot.cloud.iam.api.model.dto.tenant.TenantDetailsDTO;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;

/**
 * <p>Description  : TenantDetailsQuery.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 4:37 PM.</p>
 */
public interface TenantDetailsQuery {

    /**
     * 获取用户租户详情
     *
     * @param username 用户名
     * @return {@link TenantDetailsResponse}
     */
    TenantDetailsResponse getUserTenantDetails(String username);

    /**
     * 获取租户详情
     *
     * @param ids id列表
     * @return {@link TenantDetailsResponse}
     */
    TenantDetailsResponse getTenantByIds(List<Long> ids);

    /**
     * 获取指定租户详情
     *
     * @param id 租户ID
     * @return {@link TenantDetailsDTO}
     */
    TenantDetailsDTO getTenantById(Long id);
}
