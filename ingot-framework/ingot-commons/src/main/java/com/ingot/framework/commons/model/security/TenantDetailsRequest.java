package com.ingot.framework.commons.model.security;

import java.io.Serializable;

import lombok.Data;

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
