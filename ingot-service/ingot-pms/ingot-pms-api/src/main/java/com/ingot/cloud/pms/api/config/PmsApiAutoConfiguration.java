package com.ingot.cloud.pms.api.config;

import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <p>Description  : PmsApiAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/14.</p>
 * <p>Time         : 上午10:01.</p>
 */
@EnableFeignClients(value = {"com.ingot.cloud.pms.api.rpc"})
public class PmsApiAutoConfiguration {
}
