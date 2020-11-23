package com.ingot.cloud.pms.api.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : ApiAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/14.</p>
 * <p>Time         : 上午10:01.</p>
 */
@Configuration
@ComponentScan(value = {"com.ingot.cloud.pms.api"})
@EnableFeignClients(value = {"com.ingot.cloud.pms.api.rpc"})
public class ApiAutoConfiguration {
}
