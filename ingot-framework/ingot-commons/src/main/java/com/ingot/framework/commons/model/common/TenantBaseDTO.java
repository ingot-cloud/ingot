package com.ingot.framework.commons.model.common;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : TenantBaseDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 08:32.</p>
 */
@Data
public class TenantBaseDTO implements Serializable {
    /**
     * 租户ID
     */
    private Long id;
    /**
     * 租户名称
     */
    private String name;
    /**
     * logo
     */
    private String avatar;
}
