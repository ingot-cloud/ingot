package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.sql.Blob;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oauth2_authorization")
public class Oauth2Authorization extends BaseModel<Oauth2Authorization> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    private String registeredClientId;

    private String principalName;

    private String authorizationGrantType;

    private String authorizedScopes;

    private Blob attributes;

    private String state;

    private Blob authorizationCodeValue;

    private LocalDateTime authorizationCodeIssuedAt;

    private LocalDateTime authorizationCodeExpiresAt;

    private Blob authorizationCodeMetadata;

    private Blob accessTokenValue;

    private LocalDateTime accessTokenIssuedAt;

    private LocalDateTime accessTokenExpiresAt;

    private Blob accessTokenMetadata;

    private String accessTokenType;

    private String accessTokenScopes;

    private Blob oidcIdTokenValue;

    private LocalDateTime oidcIdTokenIssuedAt;

    private LocalDateTime oidcIdTokenExpiresAt;

    private Blob oidcIdTokenMetadata;

    private Blob refreshTokenValue;

    private LocalDateTime refreshTokenIssuedAt;

    private LocalDateTime refreshTokenExpiresAt;

    private Blob refreshTokenMetadata;

    private Blob userCodeValue;

    private LocalDateTime userCodeIssuedAt;

    private LocalDateTime userCodeExpiresAt;

    private Blob userCodeMetadata;

    private Blob deviceCodeValue;

    private LocalDateTime deviceCodeIssuedAt;

    private LocalDateTime deviceCodeExpiresAt;

    private Blob deviceCodeMetadata;
}
