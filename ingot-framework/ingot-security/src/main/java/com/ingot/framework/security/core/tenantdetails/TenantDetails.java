package com.ingot.framework.security.core.tenantdetails;

import com.ingot.framework.commons.model.common.AllowTenantDTO;

import java.util.List;

/**
 * <p>Description  : TenantDetails.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:45 PM.</p>
 */
public interface TenantDetails {

    /**
     * 获取允许的访问的租户列表
     *
     * @return {@link AllowTenantDTO}
     */
    List<AllowTenantDTO> getAllow();
}
