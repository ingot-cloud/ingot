package com.ingot.cloud.authold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <p>Description  : IngotAcsApplication.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/27.</p>
 * <p>Time         : 11:37 上午.</p>
 */
@EnableFeignClients("com.ingot.cloud.pms.api.rpc")
@EnableDiscoveryClient
@SpringBootApplication
public class IngotAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngotAuthApplication.class, args);
    }
}
