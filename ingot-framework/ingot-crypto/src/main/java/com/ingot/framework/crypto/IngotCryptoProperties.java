package com.ingot.framework.crypto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * <p>Description  : IngotCryptoProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:30 AM.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.crypto")
public class IngotCryptoProperties {
    /**
     * url参数加解密key
     */
    private String paramKey = "data";
    /**
     * body参数加解密key
     */
    private String bodyKey = "data";
    /**
     * 秘钥
     */
    private Map<String, String> secretKeys;
}
