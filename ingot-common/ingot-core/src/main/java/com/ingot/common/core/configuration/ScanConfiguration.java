package com.ingot.common.core.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : ScanConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 10:51 上午.</p>
 */
@Configuration
@ComponentScan(value = {
        "com.ingot.common.core"
})
public class ScanConfiguration {
}
