package com.ingot.framework.core.config;

import com.ingot.framework.core.utils.crypto.IngotCryptoProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>Description  : CryptConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/30.</p>
 * <p>Time         : 3:37 PM.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IngotCryptoProperties.class)
public class CryptConfig {
}
