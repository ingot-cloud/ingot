package com.ingot.example.ops;

import com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <p>独立服务启动样板，鉴权与在线会话沿用框架配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@SpringBootApplication
@EnableFeignClients(clients = RemoteIamAuthorizationService.class)
public class OpsApplication {

    /**
     * 启动示例。
     * @param args Spring参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OpsApplication.class, args);
    }

}
