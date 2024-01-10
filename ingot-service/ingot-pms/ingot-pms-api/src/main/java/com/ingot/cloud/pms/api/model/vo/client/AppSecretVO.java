package com.ingot.cloud.pms.api.model.vo.client;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : AppSecretVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/10.</p>
 * <p>Time         : 08:56.</p>
 */
@Data
public class AppSecretVO implements Serializable {
    /**
     * 应用ID
     */
    private String appId;
    /**
     * 应用秘钥
     */
    private String appSecret;
}
