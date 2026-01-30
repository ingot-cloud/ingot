package com.ingot.cloud.credential;

import com.ingot.framework.openapi.EnableOpenAPI;
import com.ingot.framework.security.config.annotation.web.configuration.EnableInWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 凭证安全服务启动类
 *
 * @author jymot
 * @since 2026-01-22
 */
@EnableOpenAPI("credential")
@EnableInWebSecurity
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class InCredentialApplication {

    public static void main(String[] args) {
        SpringApplication.run(InCredentialApplication.class, args);
    }
}
