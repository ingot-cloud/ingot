package com.ingot.cloud.auth.model.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : UserTokenQueryDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/21.</p>
 * <p>Time         : 13:31.</p>
 */
@Data
public class UserTokenQueryDTO implements Serializable {
    private Long tenantId;
    private String clientId;
    private long current;
    private long size;
}
