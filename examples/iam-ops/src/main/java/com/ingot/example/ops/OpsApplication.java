package com.ingot.example.ops;

import com.ingot.framework.security.config.annotation.web.configuration.EnableInWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>独立服务启动样板，鉴权与在线会话沿用框架配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@SpringBootApplication
@EnableInWebSecurity
public class OpsApplication {

    /**
     * 启动示例。
     * @param args Spring参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OpsApplication.class, args);
    }

}
