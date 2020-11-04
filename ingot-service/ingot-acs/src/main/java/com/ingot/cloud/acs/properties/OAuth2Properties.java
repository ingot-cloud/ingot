package com.ingot.cloud.acs.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : OAuth2Properties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 11:12 上午.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ingot.oauth2")
public class OAuth2Properties {
    private String rsaSecret;
}
