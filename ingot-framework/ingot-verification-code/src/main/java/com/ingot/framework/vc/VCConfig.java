package com.ingot.framework.vc;

import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>Description  : VCConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/21.</p>
 * <p>Time         : 10:07 PM.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IngotVCProperties.class)
public class VCConfig {
}
