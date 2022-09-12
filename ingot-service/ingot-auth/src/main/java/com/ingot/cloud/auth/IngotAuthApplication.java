package com.ingot.cloud.auth;

import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration.EnableIngotAuthorizationServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>Description  : IngotAuthApplication.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/3.</p>
 * <p>Time         : 4:55 下午.</p>
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableIngotAuthorizationServer
public class IngotAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngotAuthApplication.class, args);
    }
}
