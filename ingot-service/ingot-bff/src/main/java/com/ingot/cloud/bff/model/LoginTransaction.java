package com.ingot.cloud.bff.model;

import java.util.List;

import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.Data;

/**
 * 登录事务，与正式 BffSession 隔离存储。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class LoginTransaction {
    private String transactionId;
    private String appId;
    private AuthorizationDomain domain;
    private String entry;
    private String stage;
    private String adminBindingId;
    private String loginBindingId;
    private String csrfToken;
    private String codeVerifier;
    private String state;
    private String authCookie;
    private List<TenantMainDTO> allows;
    private String ticket;
    private long ticketExpiresAt;
    private String accessToken;
    private String refreshToken;
    private String sid;
    private Long userId;
    private String tenantId;
    private long expiresAt;
}
