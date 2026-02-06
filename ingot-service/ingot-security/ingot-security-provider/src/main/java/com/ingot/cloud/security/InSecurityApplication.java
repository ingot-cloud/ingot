package com.ingot.cloud.security;

import com.ingot.framework.openapi.EnableOpenAPI;
import com.ingot.framework.security.config.annotation.web.configuration.EnableInWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 安全中心
 *
 * @author jymot
 * @since 2026-01-22
 */
@EnableOpenAPI("security")
@EnableInWebSecurity
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class InSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(InSecurityApplication.class, args);
    }
}
