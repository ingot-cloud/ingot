package com.ingot.cloud.auth;

import com.ingot.framework.openapi.EnableOpenAPI;
import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration.EnableInAuthorizationServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>Description  : 授权服务应用.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/3.</p>
 * <p>Time         : 4:55 下午.</p>
 */
@EnableOpenAPI("auth")
@EnableDiscoveryClient
@SpringBootApplication
@EnableInAuthorizationServer
public class InAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(InAuthApplication.class, args);
    }
}
