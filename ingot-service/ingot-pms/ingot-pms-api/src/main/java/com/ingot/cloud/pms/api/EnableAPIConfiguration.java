package com.ingot.cloud.pms.api;

import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <p>Description  : EnableApiAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/28.</p>
 * <p>Time         : 11:33 上午.</p>
 */
@AutoConfiguration
@EnableFeignClients
@ConditionalOnClass(Feign.class)
public class EnableAPIConfiguration {
}
