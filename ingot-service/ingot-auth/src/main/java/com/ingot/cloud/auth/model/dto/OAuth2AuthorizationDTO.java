package com.ingot.cloud.auth.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * <p>Description  : OAuth2AuthorizationDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/26.</p>
 * <p>Time         : 1:56 下午.</p>
 */
@Data
public class OAuth2AuthorizationDTO implements Serializable {
    private String id;
    private String registeredClientId;
    private String principalName;
    private String authorizationGrantType;
    private LocalDateTime tokenIssuedAt;
    private LocalDateTime tokenExpiresAt;
}
