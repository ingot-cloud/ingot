package com.ingot.cloud.pms.api.model.dto.token;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : TokenDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/26.</p>
 * <p>Time         : 下午1:15.</p>
 */
@Data
public class TokenDto implements Serializable {
    /**
     * token
     */
    private String accessToken;
    /**
     * token 过期时间，单位秒
     */
    private int accessTokenExpire;
    /**
     * 刷新 token
     */
    private String refreshToken;
    /**
     * 刷新 token 过期时间
     */
    private int refreshTokenExpire;
    /**
     * 重定向Url
     */
    private String redirectUrl;
}
