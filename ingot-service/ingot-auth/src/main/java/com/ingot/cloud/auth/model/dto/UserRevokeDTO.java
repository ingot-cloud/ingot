package com.ingot.cloud.auth.model.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : UserRevokeDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/21.</p>
 * <p>Time         : 12:36.</p>
 */
@Data
public class UserRevokeDTO implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 客户端ID
     */
    private Long clientId;
    /**
     * 组织ID
     */
    private Long tenantId;
}
