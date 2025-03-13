package com.ingot.framework.core.model.security;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : TenantDetailsRequest.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:57 PM.</p>
 */
@Data
public class TenantDetailsRequest implements Serializable {
    private String username;
}
