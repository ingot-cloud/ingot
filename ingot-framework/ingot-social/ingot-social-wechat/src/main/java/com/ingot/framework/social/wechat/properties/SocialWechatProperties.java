package com.ingot.framework.social.wechat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : WechatProperties.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/19.</p>
 * <p>Time         : 10:44.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.social.wechat")
public class SocialWechatProperties {

    /**
     * 默认激活的微信小程序ID
     */
    private String miniProgramAppId;
}
