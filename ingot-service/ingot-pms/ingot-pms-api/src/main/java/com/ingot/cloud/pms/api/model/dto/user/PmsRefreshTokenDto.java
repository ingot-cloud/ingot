package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : RefreshTokenDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/5.</p>
 * <p>Time         : 5:37 PM.</p>
 */
@Data
public class PmsRefreshTokenDto implements Serializable {
    /**
     * 原来的访问token
     */
    private String accessToken;
    /**
     * 新的刷新token
     */
    private String newAccessToken;
    /**
     * 新token有效时间
     */
    private int expireIn;
    /**
     * 客户端操作系统
     */
    private String os;
    /**
     * 获取客户端浏览器
     */
    private String browser;
    /**
     * 访问者IP
     */
    private String remoteIP;
    /**
     * 远程地址
     */
    private String remoteLocation;
}
