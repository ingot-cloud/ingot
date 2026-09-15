package com.ingot.cloud.iam;

import com.ingot.framework.openapi.EnableOpenAPI;
import com.ingot.framework.security.config.annotation.web.configuration.EnableInWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>启动 IAM 服务，装配身份管理、资源目录及授权相关组件。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@EnableOpenAPI("iam")
@EnableInWebSecurity
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class InIamApplication {

    /**
     * 使用 Spring Boot 启动服务。
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(InIamApplication.class, args);
    }
}
