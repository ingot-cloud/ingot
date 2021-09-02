package com.ingot.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>Description  : IngotGatewayApplication.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 8:01 下午.</p>
 */
@EnableDiscoveryClient
@SpringBootApplication
public class IngotGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(IngotGatewayApplication.class, args);
    }
}
