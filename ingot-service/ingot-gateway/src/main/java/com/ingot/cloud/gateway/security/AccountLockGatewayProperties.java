package com.ingot.cloud.gateway.security;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Gateway 账号锁定 Filter 配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.account-lock-gateway")
public class AccountLockGatewayProperties {

    /**
     * 是否启用账号锁定 Filter，默认开启。
     */
    private boolean enabled = true;

    /**
     * 不检查锁定状态的路径 Ant 模式。
     */
    private List<String> excludePathPatterns = new ArrayList<>(List.of(
            "/actuator/**",
            "/bff/auth/login",
            "/bff/auth/**"
    ));
}
