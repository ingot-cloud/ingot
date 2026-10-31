package com.ingot.cloud.bff.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>
 * BFF UTC 时钟与可靠清理任务装配。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class BffConfig {

    /** 安全期限统一使用 UTC 时钟，可在隔离测试中替换。 */
    @Bean
    public Clock bffClock() {
        return Clock.systemUTC();
    }

}
