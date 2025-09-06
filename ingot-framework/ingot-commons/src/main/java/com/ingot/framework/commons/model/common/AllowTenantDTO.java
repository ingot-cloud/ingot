package com.ingot.framework.commons.model.common;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : AllowTenantDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:35 PM.</p>
 */
@Data
public class AllowTenantDTO implements Serializable {
    /**
     * 租户ID
     */
    private String id;
    /**
     * 租户名称
     */
    private String name;
    /**
     * logo
     */
    private String avatar;
    /**
     * 是否为主要租户
     */
    private Boolean main;
}
