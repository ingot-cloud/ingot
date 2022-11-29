package com.ingot.cloud.pms;

import com.ingot.framework.security.config.annotation.web.configuration.EnableIngotWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>Description  : IngotPmsApplication.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:15 下午.</p>
 */
@EnableIngotWebSecurity
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class IngotPmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngotPmsApplication.class, args);
    }
}
