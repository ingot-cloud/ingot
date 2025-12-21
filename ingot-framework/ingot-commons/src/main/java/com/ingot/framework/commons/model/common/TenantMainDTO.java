package com.ingot.framework.commons.model.common;

import java.io.Serial;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AllowTenantDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:35 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TenantMainDTO extends TenantBaseDTO {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否为主要租户
     */
    private Boolean main;
}
