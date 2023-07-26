package com.ingot.framework.security.core.tenantdetails;

import cn.hutool.core.collection.ListUtil;
import com.ingot.framework.core.model.dto.common.AllowTenantDTO;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * <p>Description  : Tenant.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:47 PM.</p>
 */
public class Tenant implements TenantDetails {

    public static final Tenant EMPTY = new Tenant(ListUtil.empty());

    @Getter
    private final List<AllowTenantDTO> allows;

    public Tenant(List<AllowTenantDTO> allows) {
        this.allows = Collections.unmodifiableList(allows);
    }

    @Override
    public List<AllowTenantDTO> getAllow() {
        return null;
    }
}
