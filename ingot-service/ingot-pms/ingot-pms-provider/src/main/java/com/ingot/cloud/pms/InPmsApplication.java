package com.ingot.cloud.pms;

import com.ingot.framework.security.config.annotation.web.configuration.EnableInWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>Description  : 权限综合管理应用.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:15 下午.</p>
 */
@EnableInWebSecurity
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class InPmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(InPmsApplication.class, args);
    }
}
