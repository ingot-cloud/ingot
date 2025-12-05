package com.ingot.cloud.member.common.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : WechatProperties.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/19.</p>
 * <p>Time         : 10:44.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.wechat")
public class WechatProperties {

    /**
     * 当前app使用的微信小程序ID
     */
    private String appMiniProgramAppId;

    /**
     * 当前admin使用的微信小程序ID
     */
    private String adminMiniProgramAppId;
}
