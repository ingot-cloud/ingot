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
}
