package com.ingot.cloud.member.api;

import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <p>Description  : EnableAPIConfiguration.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 14:33.</p>
 */
@AutoConfiguration
@EnableFeignClients
@ConditionalOnClass(Feign.class)
public class EnableAPIConfiguration {
}
