package com.ingot.cloud.pms.api.model.dto.client;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableId;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.validation.Group;
import lombok.Data;

/**
 * <p>Description  : OAuth2RegisteredClientDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/11.</p>
 * <p>Time         : 2:16 下午.</p>
 */
@Data
public class OAuth2RegisteredClientDto implements Serializable {
    /**
     * ID
     */
    @TableId
    private String id;

    /**
     * 客户端ID
     */
    @NotBlank(message = "{Oauth2RegisteredClient.clientId}", groups = Group.Create.class)
    private String clientId;

    /**
     * 客户端秘钥
     */
    @NotBlank(message = "{Oauth2RegisteredClient.clientSecret}", groups = Group.Create.class)
    private String clientSecret;

    /**
     * 秘钥过期时间
     */
    private LocalDateTime clientSecretExpiresAt;

    /**
     * 客户端名称
     */
    @NotBlank(message = "{Oauth2RegisteredClient.clientName}", groups = Group.Create.class)
    private String clientName;

    /**
     * 客户端认证方法
     */
    private String clientAuthenticationMethods;

    /**
     * 客户端可以使用的授权类型
     */
    private String authorizationGrantTypes;

    /**
     * 重定向URL
     */
    private String redirectUris;

    /**
     * 客户端的访问范围
     */
    private String scopes;

    /**
     * 如果客户端在执行授权码授权流程时需要提供验证密钥质询和验证器，则设置为true。
     */
    private Boolean requireProofKey;

    /**
     * 客户端请求访问时需要授权同意，则设置为true。
     */
    private Boolean requireAuthorizationConsent;

    /**
     * Token存活时间
     */
    private String accessTokenTimeToLive;

    /**
     * 是否重复使用RefreshToken
     */
    private Boolean reuseRefreshTokens;

    /**
     * RefreshToken存活时间
     */
    private String refreshTokenTimeToLive;

    /**
     * 设置签名ID令牌的JWS算法
     */
    private String idTokenSignatureAlgorithm;

    /**
     * token认证方法
     */
    private String tokenAuthenticationMethod;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;
}
