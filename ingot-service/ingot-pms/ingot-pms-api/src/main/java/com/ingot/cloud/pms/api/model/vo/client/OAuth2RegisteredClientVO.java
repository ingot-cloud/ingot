package com.ingot.cloud.pms.api.model.vo.client;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.framework.core.model.enums.CommonStatusEnum;
import lombok.Data;

/**
 * <p>Description  : OAuth2RegisteredClientVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/14.</p>
 * <p>Time         : 5:28 下午.</p>
 */
@Data
public class OAuth2RegisteredClientVO implements Serializable {
    /**
     * ID
     */
    private String id;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * client id 发布时间
     */
    private LocalDateTime clientIdIssuedAt;

    /**
     * 客户端秘钥
     */
    private String clientSecret;

    /**
     * 客户端名称
     */
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
