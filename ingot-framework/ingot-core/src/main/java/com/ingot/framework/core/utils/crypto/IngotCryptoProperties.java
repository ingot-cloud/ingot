package com.ingot.framework.core.utils.crypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * <p>Description  : IngotCryptoProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/30.</p>
 * <p>Time         : 3:24 PM.</p>
 */
@Data
@RefreshScope
@ConfigurationProperties("ingot.crypto")
public class IngotCryptoProperties {
    /**
     * AES key
     */
    private String aesKey;
}
