package com.ingot.cloud.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>Description  : InTestApplication.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/31.</p>
 * <p>Time         : 16:47.</p>
 */
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class InTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(InTestApplication.class, args);
    }
}
