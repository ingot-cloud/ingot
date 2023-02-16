package com.ingot.framework.security.oauth2.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : IngotOAuth2AuthProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 2:59 下午.</p>
 */
@ConfigurationProperties(prefix = "ingot.security.oauth2.auth")
public class IngotOAuth2AuthProperties {
    @Getter
    @Setter
    private String issuer;
}
