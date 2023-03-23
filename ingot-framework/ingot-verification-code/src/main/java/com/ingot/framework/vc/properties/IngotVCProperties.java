package com.ingot.framework.vc.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : IngotVCProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/23.</p>
 * <p>Time         : 7:40 PM.</p>
 */
@Data
@ConfigurationProperties("ingot.vc")
public class IngotVCProperties {
    /**
     * 图片验证码配置
     */
    private ImageCodeProperties image = new ImageCodeProperties();
    /**
     * 短信验证码配置
     */
    private SMSCodeProperties sms = new SMSCodeProperties();
    /**
     * 邮箱验证码配置
     */
    private EmailCodeProperties email = new EmailCodeProperties();
}
