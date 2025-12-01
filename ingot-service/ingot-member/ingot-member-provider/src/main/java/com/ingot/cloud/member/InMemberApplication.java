package com.ingot.cloud.member;

import com.ingot.framework.openapi.EnableOpenAPI;
import com.ingot.framework.security.config.annotation.web.configuration.EnableInWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>Description  : 会员服务应用.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@EnableOpenAPI("member")
@EnableInWebSecurity
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class InMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(InMemberApplication.class, args);
    }
}

