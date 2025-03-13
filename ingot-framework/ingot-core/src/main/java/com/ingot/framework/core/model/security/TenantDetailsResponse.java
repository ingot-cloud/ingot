package com.ingot.framework.core.model.security;

import com.ingot.framework.core.model.common.AllowTenantDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : TenantDetailsResponse.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:58 PM.</p>
 */
@Data
public class TenantDetailsResponse implements Serializable {
    private List<AllowTenantDTO> allows;
}
