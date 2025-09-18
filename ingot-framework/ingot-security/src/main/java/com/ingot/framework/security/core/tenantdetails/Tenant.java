package com.ingot.framework.security.core.tenantdetails;

import java.util.Collections;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import com.ingot.framework.commons.model.common.AllowTenantDTO;

/**
 * <p>Description  : Tenant.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:47 PM.</p>
 */
public class Tenant implements TenantDetails {

    public static final Tenant EMPTY = new Tenant(ListUtil.empty());

    private final List<AllowTenantDTO> allows;

    public Tenant(List<AllowTenantDTO> allows) {
        this.allows = Collections.unmodifiableList(allows);
    }

    @Override
    public List<AllowTenantDTO> getAllow() {
        return allows;
    }
}
