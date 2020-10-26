package com.ingot.id.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : IdProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 3:31 下午.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.id")
public class IdProperties {

    private String localPathPrefix = "/data/ingot/id/";
    private String mode;
}
